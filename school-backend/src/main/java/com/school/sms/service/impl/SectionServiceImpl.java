package com.school.sms.service.impl;

import com.school.sms.dto.request.AssignClassTeacherRequest;
import com.school.sms.dto.request.SectionRequest;
import com.school.sms.dto.response.SectionDto;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.StudentStatus;
import com.school.sms.entity.Teacher;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.SectionMapper;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.service.SectionService;
import com.school.sms.service.UserService;
import com.school.sms.util.AppConstants;
import com.school.sms.util.NameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TeacherRepository teacherRepository;
    private final SectionMapper sectionMapper;
    private final StudentRepository studentRepository;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public List<SectionDto> getByClassId(Long classId) {
        findClass(classId);
        return sectionRepository.findAllBySchoolClassIdOrderBySectionNameAsc(classId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SectionDto create(Long classId, SectionRequest request) {
        SchoolClass schoolClass = findClass(classId);
        if (sectionRepository.existsBySchoolClassIdAndSectionNameIgnoreCase(classId, request.getSectionName())) {
            throw new DuplicateResourceException("Section", "sectionName", request.getSectionName());
        }
        verifySingleSectionRule(classId, request.getSectionName());

        Section entity = sectionMapper.toEntity(request);
        entity.setSchoolClass(schoolClass);

        return toDto(sectionRepository.save(entity));
    }

    @Override
    @Transactional
    public SectionDto update(Long id, SectionRequest request) {
        Section entity = findEntity(id);

        boolean nameChanged = !entity.getSectionName().equalsIgnoreCase(request.getSectionName());
        if (nameChanged && sectionRepository.existsBySchoolClassIdAndSectionNameIgnoreCase(
                entity.getSchoolClass().getId(), request.getSectionName())) {
            throw new DuplicateResourceException("Section", "sectionName", request.getSectionName());
        }

        sectionMapper.updateEntityFromRequest(request, entity);
        return toDto(sectionRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        sectionRepository.delete(findEntity(id));
    }

    @Override
    @Transactional
    public SectionDto assignClassTeacher(Long id, AssignClassTeacherRequest request) {
        Section entity = findEntity(id);
        Teacher previous = entity.getClassTeacher();

        if (request.getTeacherId() == null) {
            entity.setClassTeacher(null);
            Section saved = sectionRepository.save(entity);
            sectionRepository.flush();
            // Unassigning used to leave the outgoing teacher on the CLASS_TEACHER
            // role for good: nothing demoted them, so they kept homeroom privileges
            // over a homeroom they no longer had. The flag is cleared here.
            refreshClassTeacherFlag(previous);
            return toDto(saved);
        }

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.getTeacherId()));

        verifyTeacherHasNoOtherHomeroom(teacher, entity);

        entity.setClassTeacher(teacher);
        Section saved = sectionRepository.save(entity);
        // Flushed before the flags are refreshed: the refresh asks the database
        // whether each teacher still holds a section, and without this it would be
        // answered from the rows as they were when the transaction started.
        sectionRepository.flush();

        refreshClassTeacherFlag(previous);
        refreshClassTeacherFlag(teacher);

        return toDto(saved);
    }

    /**
     * Brings {@code teachers.is_class_teacher} back in line with the section rows.
     *
     * <p>This is what replaced promoting a teacher to the CLASS_TEACHER role. The
     * flag grants the six permissions in {@code class_teacher_permissions} on the
     * caller's next request — see {@code CustomUserDetailsService} — so the teacher
     * keeps one role for their whole career and the duty comes and goes with the
     * assignment.
     *
     * <p>Derived from the section rows rather than set to a literal true/false,
     * which costs one indexed lookup and makes the two impossible to disagree: the
     * question "does this teacher head a section" has exactly one answer and it
     * lives in {@code sections}. {@code uq_sections_class_teacher} allows at most
     * one, so the outgoing teacher normally holds none by the time this runs — but
     * asking is cheaper than assuming, and it is the same query the migration's
     * drift check uses.
     */
    private void refreshClassTeacherFlag(Teacher teacher) {
        if (teacher == null) {
            return;
        }
        boolean holdsASection = sectionRepository.findByClassTeacherId(teacher.getId()).isPresent();
        if (teacher.isClassTeacher() != holdsASection) {
            teacher.setClassTeacher(holdsASection);
            teacherRepository.save(teacher);
        }
    }

    /**
     * One section per class, named "A".
     *
     * <p>Enforced here rather than by a CHECK constraint so the rule can be lifted
     * by changing this class alone if the school ever streams a year again — the
     * schema keeps its section dimension either way.
     *
     * <p>A wrong name is rejected rather than quietly rewritten to "A": a caller
     * that asked for "B" has a different model of the school in mind, and silently
     * storing something else would hide that rather than settle it.
     */
    private void verifySingleSectionRule(Long classId, String requestedName) {
        if (!AppConstants.SINGLE_SECTION_NAME.equalsIgnoreCase(requestedName)) {
            throw new BadRequestException("This school runs a single section per class, named "
                    + AppConstants.SINGLE_SECTION_NAME + "; \"" + requestedName + "\" cannot be created");
        }
        if (!sectionRepository.findAllBySchoolClassIdOrderBySectionNameAsc(classId).isEmpty()) {
            throw new BadRequestException("This class already has its section. "
                    + "One section per class is the rule, so add subjects to the existing section instead.");
        }
    }

    /**
     * A teacher is homeroom of at most one section.
     *
     * <p>uq_sections_class_teacher enforces this in the database, but a
     * duplicate-key error surfaces as a 500 naming an index. Checking first turns
     * it into a 400 that says which section the teacher already holds, which is
     * the thing the administrator needs in order to decide what to do.
     *
     * <p>Re-assigning a teacher to the section they already head is a no-op rather
     * than an error — the grid sends the current value back when another field in
     * the row is edited.
     */
    private void verifyTeacherHasNoOtherHomeroom(Teacher teacher, Section target) {
        sectionRepository.findByClassTeacherId(teacher.getId())
                .filter(existing -> !existing.getId().equals(target.getId()))
                .ifPresent(existing -> {
                    String teacherName = teacher.getUser() != null
                            ? NameUtil.fullName(teacher.getUser().getFirstName(), teacher.getUser().getLastName())
                            : "That teacher";
                    String where = existing.getSchoolClass() != null
                            ? existing.getSchoolClass().getClassName() + " - " + existing.getSectionName()
                            : "section " + existing.getSectionName();
                    throw new BadRequestException(teacherName + " is already the class teacher of " + where
                            + ". A teacher can be class teacher of only one section — free that one first.");
                });
    }

    private SectionDto toDto(Section section) {
        SectionDto dto = sectionMapper.toDto(section);
        if (section.getClassTeacher() != null) {
            Teacher teacher = section.getClassTeacher();
            dto.setClassTeacherName(NameUtil.fullName(teacher.getUser().getFirstName(), teacher.getUser().getLastName()));
        }
        // Strength drives the capacity readout beside it, so it ships with every
        // section rather than being fetched separately and risking the two disagreeing.
        dto.setStudentCount((int) studentRepository
                .countBySectionIdAndDeletedFalseAndStatus(section.getId(), StudentStatus.ACTIVE));
        return dto;
    }

    private Section findEntity(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", id));
    }

    private SchoolClass findClass(Long classId) {
        return schoolClassRepository.findById(classId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
    }
}

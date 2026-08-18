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

        if (request.getTeacherId() == null) {
            entity.setClassTeacher(null);
            Section saved = sectionRepository.save(entity);
            return toDto(saved);
        }

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.getTeacherId()));

        verifyTeacherHasNoOtherHomeroom(teacher, entity);

        entity.setClassTeacher(teacher);
        Section saved = sectionRepository.save(entity);

        userService.promoteToClassTeacher(teacher.getUser().getId());

        return toDto(saved);
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

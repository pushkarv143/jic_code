package com.school.sms.service.impl;

import com.school.sms.dto.request.ClassOfficialRequest;
import com.school.sms.dto.response.ClassOfficialDto;
import com.school.sms.entity.ClassOfficial;
import com.school.sms.entity.ClassOfficialRole;
import com.school.sms.entity.Gender;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentStatus;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.ClassOfficialRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.ClassOfficialService;
import com.school.sms.util.NameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClassOfficialServiceImpl implements ClassOfficialService {

    private final ClassOfficialRepository classOfficialRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<ClassOfficialDto> getCurrent(Long classId) {
        findClass(classId);
        return classOfficialRepository.findAllBySchoolClassIdAndToDateIsNullOrderByRoleAsc(classId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassOfficialDto> getHistory(Long classId) {
        findClass(classId);
        return classOfficialRepository.findAllBySchoolClassIdOrderByFromDateDescIdDesc(classId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public ClassOfficialDto appoint(Long classId, ClassOfficialRequest request) {
        SchoolClass schoolClass = findClass(classId);
        Student student = findActiveStudent(request.getStudentId());

        // A post belongs to the class it is a post of. Without this an admin could
        // make a Class 12 student the head boy of Class 3 — the foreign keys are
        // satisfied, so nothing else would object.
        verifyStudentBelongsToClass(student, schoolClass);
        verifyGenderMatchesRole(student, request.getRole());
        verifyStudentHoldsNoOtherPost(student, request.getRole(), classId);

        Section section = null;
        if (request.getSectionId() != null) {
            section = sectionRepository.findById(request.getSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Section", "id", request.getSectionId()));
            if (section.getSchoolClass() == null || !section.getSchoolClass().getId().equals(classId)) {
                throw new BadRequestException("That section does not belong to this class");
            }
        }

        LocalDate fromDate = request.getFromDate() != null ? request.getFromDate() : LocalDate.now();

        // Succession, not replacement: the sitting holder's tenure is closed the day
        // before the successor starts so the history reads as a continuous line, and
        // so the database's one-current-holder constraint is free by the time the new
        // row is inserted. Closing it the day before rather than on `fromDate` keeps
        // the two tenures from both covering that date.
        Optional<ClassOfficial> sitting =
                classOfficialRepository.findBySchoolClassIdAndRoleAndToDateIsNull(classId, request.getRole());
        if (sitting.isPresent()) {
            // The sitting holder is necessarily someone else by this point:
            // verifyStudentHoldsNoOtherPost above rejects re-appointing whoever
            // already holds it, so this branch only ever handles a real handover.
            ClassOfficial outgoing = sitting.get();
            LocalDate end = fromDate.minusDays(1);
            // A same-day handover would otherwise end the outgoing tenure before it
            // began; anchor it to its own start instead of going negative.
            outgoing.setToDate(end.isBefore(outgoing.getFromDate()) ? outgoing.getFromDate() : end);
            classOfficialRepository.saveAndFlush(outgoing);
        }

        ClassOfficial official = ClassOfficial.builder()
                .schoolClass(schoolClass)
                .section(section)
                .student(student)
                .role(request.getRole())
                .fromDate(fromDate)
                .remarks(request.getRemarks())
                .build();

        ClassOfficial saved = classOfficialRepository.save(official);
        auditLogService.record("APPOINT_CLASS_OFFICIAL", "ClassOfficial", saved.getId(), null, null);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void end(Long classId, Long officialId) {
        ClassOfficial official = classOfficialRepository.findById(officialId)
                .orElseThrow(() -> new ResourceNotFoundException("Class official", "id", officialId));

        if (official.getSchoolClass() == null || !official.getSchoolClass().getId().equals(classId)) {
            throw new BadRequestException("That appointment does not belong to this class");
        }
        if (!official.isCurrent()) {
            throw new BadRequestException("That appointment has already ended");
        }

        // Never before it started: ending a post appointed for a future date closes
        // it at its own start rather than recording a negative tenure.
        LocalDate today = LocalDate.now();
        official.setToDate(today.isBefore(official.getFromDate()) ? official.getFromDate() : today);
        classOfficialRepository.save(official);
        auditLogService.record("END_CLASS_OFFICIAL", "ClassOfficial", officialId, null, null);
    }

    /**
     * A student holds at most one post at a time.
     *
     * <p>uq_class_official_student_current enforces it in the database
     * (13_assignment_uniqueness.sql); this turns the duplicate-key error into a
     * message naming the post they already hold. Without the rule the same student
     * could be head boy and monitor at once, which is not a thing a school does and
     * is almost always the wrong name picked from a long list.
     *
     * <p>Re-appointing to the post already held is reported as such rather than as
     * a clash, because it is a different mistake with a different fix. Note the
     * check spans classes even though a student belongs to only one — the guard
     * stays correct if a student is ever moved mid-term while holding a post.
     */
    private void verifyStudentHoldsNoOtherPost(Student student, ClassOfficialRole wantedRole, Long classId) {
        for (ClassOfficial held : classOfficialRepository.findAllByStudentIdAndToDateIsNull(student.getId())) {
            boolean samePost = held.getRole() == wantedRole
                    && held.getSchoolClass() != null
                    && held.getSchoolClass().getId().equals(classId);
            if (samePost) {
                throw new BadRequestException("That student already holds this post");
            }

            String name = studentName(student);
            String where = held.getSchoolClass() != null ? held.getSchoolClass().getClassName() : "another class";
            throw new BadRequestException((name != null ? name : "That student") + " is already "
                    + humanise(held.getRole()) + " of " + where
                    + ". A student can hold only one post at a time — end that one first.");
        }
    }

    /**
     * HEAD_BOY requires a male student and HEAD_GIRL a female one; the other posts
     * are open to anyone. A mismatch is almost always the wrong student picked from
     * a long list, so it is rejected rather than warned about.
     */
    private void verifyGenderMatchesRole(Student student, ClassOfficialRole role) {
        Gender required = role.requiredGender();
        if (required == null) {
            return;
        }
        if (student.getGender() == null) {
            throw new BadRequestException("That student has no gender recorded, so they cannot be made "
                    + humanise(role) + ". Set their gender first.");
        }
        if (student.getGender() != required) {
            throw new BadRequestException(humanise(role) + " must be a "
                    + required.name().toLowerCase() + " student");
        }
    }

    private void verifyStudentBelongsToClass(Student student, SchoolClass schoolClass) {
        if (student.getSchoolClass() == null || !student.getSchoolClass().getId().equals(schoolClass.getId())) {
            throw new BadRequestException("That student is not enrolled in this class");
        }
    }

    private String humanise(ClassOfficialRole role) {
        String[] words = role.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private SchoolClass findClass(Long id) {
        return schoolClassRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
    }

    private Student findActiveStudent(Long id) {
        Student student = studentRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new BadRequestException("Only an active student can hold a class post");
        }
        return student;
    }

    private ClassOfficialDto toDto(ClassOfficial official) {
        Student student = official.getStudent();
        Section section = official.getSection();
        return ClassOfficialDto.builder()
                .id(official.getId())
                .classId(official.getSchoolClass() != null ? official.getSchoolClass().getId() : null)
                .className(official.getSchoolClass() != null ? official.getSchoolClass().getClassName() : null)
                .sectionId(section != null ? section.getId() : null)
                .sectionName(section != null ? section.getSectionName() : null)
                .studentId(student != null ? student.getId() : null)
                .studentName(student != null ? studentName(student) : null)
                .rollNumber(student != null ? student.getRollNumber() : null)
                .admissionNumber(student != null ? student.getAdmissionNumber() : null)
                .role(official.getRole() != null ? official.getRole().name() : null)
                .fromDate(official.getFromDate())
                .toDate(official.getToDate())
                .current(official.isCurrent())
                .remarks(official.getRemarks())
                .build();
    }

    /**
     * Identity from the student, falling back to the login account — the same rule
     * the attendance responses follow, since students.user_id is nullable and a
     * student admitted without a login still has a name of their own.
     */
    private String studentName(Student student) {
        if (StringUtils.hasText(student.getFirstName())) {
            return NameUtil.fullName(student.getFirstName(), student.getLastName());
        }
        return student.getUser() != null
                ? NameUtil.fullName(student.getUser().getFirstName(), student.getUser().getLastName())
                : null;
    }
}

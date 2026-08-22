package com.school.sms.service.impl;

import com.school.sms.dto.request.MarkStudentAttendanceRequest;
import com.school.sms.dto.request.StudentAttendanceRecordItem;
import com.school.sms.dto.response.MonthlyAttendanceRowDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentAttendanceRecordDto;
import com.school.sms.dto.response.StudentAttendanceRowDto;
import com.school.sms.dto.response.StudentAttendanceSummaryDto;
import com.school.sms.entity.AttendanceStatus;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentAttendance;
import com.school.sms.entity.StudentStatus;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentAttendanceRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.SectionAccessGuard;
import com.school.sms.security.SecurityUtils;
import com.school.sms.security.StudentAccessGuard;
import com.school.sms.service.StudentAttendanceService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudentAttendanceServiceImpl implements StudentAttendanceService {

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final StudentAccessGuard studentAccessGuard;
    private final SectionAccessGuard sectionAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public List<StudentAttendanceRowDto> getGrid(Long classId, Long sectionId, LocalDate date) {
        sectionAccessGuard.verifyCanAccessSection(classId, sectionId);

        List<Student> students = studentRepository
                .findAllBySchoolClassIdAndSectionIdAndDeletedFalseAndStatusOrderByRollNumberAsc(
                        classId, sectionId, StudentStatus.ACTIVE);

        Map<Long, StudentAttendance> byStudentId = new HashMap<>();
        if (!students.isEmpty()) {
            List<Long> studentIds = students.stream().map(Student::getId).toList();
            studentAttendanceRepository.findAllByStudentIdInAndAttendanceDate(studentIds, date)
                    .forEach(record -> byStudentId.put(record.getStudent().getId(), record));
        }

        return students.stream()
                .map(student -> {
                    StudentAttendance record = byStudentId.get(student.getId());
                    return StudentAttendanceRowDto.builder()
                            .studentId(student.getId())
                            .firstName(firstNameOf(student))
                            .lastName(lastNameOf(student))
                            .rollNumber(student.getRollNumber())
                            .status(record != null ? record.getStatus().name() : null)
                            .remarks(record != null ? record.getRemarks() : null)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public int mark(MarkStudentAttendanceRequest request) {
        // Checked before anything is read or written: marking is a batch operation,
        // so an unauthorised caller must be stopped up front rather than part-way
        // through a partially-applied batch.
        //
        // The homeroom check, not the wider section one. Taking the register is the
        // class teacher's job — a subject teacher who happens to teach this section
        // may read its attendance and enter its marks, but not mark it present.
        // Management keeps its pass so the office can still correct a register.
        sectionAccessGuard.verifyIsHomeroomOrManagement(request.getClassId(), request.getSectionId());

        SchoolClass schoolClass = findClass(request.getClassId());
        Section section = findSection(request.getSectionId());
        Long markedBy = SecurityUtils.getCurrentUserId();

        int count = 0;
        for (StudentAttendanceRecordItem item : request.getRecords()) {
            Student student = studentRepository.findById(item.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", item.getStudentId()));

            // The class/section on the record comes from the request, not from the
            // student, so without this a caller authorised for section A could submit
            // a student from section B and have their attendance filed under A —
            // corrupting the roster and side-stepping the section check above.
            verifyStudentBelongsToSection(student, schoolClass, section);

            StudentAttendance record = studentAttendanceRepository
                    .findByStudentIdAndAttendanceDate(student.getId(), request.getAttendanceDate())
                    .orElseGet(() -> StudentAttendance.builder()
                            .student(student)
                            .schoolClass(schoolClass)
                            .section(section)
                            .attendanceDate(request.getAttendanceDate())
                            .build());

            record.setSchoolClass(schoolClass);
            record.setSection(section);
            record.setStatus(item.getStatus());
            record.setRemarks(item.getRemarks());
            record.setMarkedBy(markedBy);

            studentAttendanceRepository.save(record);
            count++;
        }
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentAttendanceRecordDto> getReport(Long studentId, Long classId, Long sectionId,
                                                               LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // Full scope, not the self-only one: a STUDENT/PARENT is held to their own
        // records and a teacher to the students they teach. When a studentId is given
        // it is checked directly; when it is omitted the caller's whole scope is
        // applied as a predicate below, so an unfiltered report cannot be used to
        // read past it.
        if (studentId != null) {
            studentAccessGuard.verifyCanViewStudentRecord(studentId);
        }

        Specification<StudentAttendance> spec = new SpecificationBuilder<StudentAttendance>()
                .with(studentId != null, "student.id", SearchOperation.EQUALS, studentId)
                .with(classId != null, "schoolClass.id", SearchOperation.EQUALS, classId)
                .with(sectionId != null, "section.id", SearchOperation.EQUALS, sectionId)
                .with(startDate != null, "attendanceDate", SearchOperation.GREATER_THAN_EQUAL, startDate)
                .with(endDate != null, "attendanceDate", SearchOperation.LESS_THAN_EQUAL, endDate)
                .build();

        // null = caller sees everyone; an empty list means they legitimately see
        // nobody and must stay empty rather than degrade into "no filter".
        List<Long> scopedIds = studentAccessGuard.resolveStudentDirectoryScope();
        if (scopedIds != null) {
            Specification<StudentAttendance> scopeSpec = scopedIds.isEmpty()
                    ? (root, query, cb) -> cb.disjunction()
                    : (root, query, cb) -> root.get("student").get("id").in(scopedIds);
            spec = spec == null ? scopeSpec : spec.and(scopeSpec);
        }

        Page<StudentAttendance> page = studentAttendanceRepository.findAll(spec, pageable);
        Page<StudentAttendanceRecordDto> dtoPage = page.map(this::toRecordDto);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceSummaryDto getSummary(Long studentId, LocalDate startDate, LocalDate endDate) {
        findEntity(studentId);
        studentAccessGuard.verifyCanViewStudentRecord(studentId);

        List<StudentAttendance> records = studentAttendanceRepository
                .findAllByStudentIdAndAttendanceDateBetween(studentId, startDate, endDate);

        long present = countByStatus(records, AttendanceStatus.PRESENT);
        long absent = countByStatus(records, AttendanceStatus.ABSENT);
        long late = countByStatus(records, AttendanceStatus.LATE);
        long halfDay = countByStatus(records, AttendanceStatus.HALF_DAY);
        long leave = countByStatus(records, AttendanceStatus.LEAVE);
        long total = records.size();

        double percentage = total == 0
                ? 0.0
                : Math.round(((present + 0.5 * halfDay) / total) * 100 * 100) / 100.0;

        return StudentAttendanceSummaryDto.builder()
                .presentDays(present)
                .absentDays(absent)
                .lateDays(late)
                .halfDays(halfDay)
                .leaveDays(leave)
                .totalMarkedDays(total)
                .percentage(percentage)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceSummaryDto getOwnSummary(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getCurrentUserId();
        Student student = studentRepository.findByUserId(userId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new BadRequestException("Your login is not linked to a student record"));

        // Reuses getSummary rather than duplicating the percentage arithmetic; the
        // guard call inside it is a no-op here since this is by definition their own
        // record, but leaving it in place keeps a single enforced path.
        return getSummary(student.getId(), startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyAttendanceRowDto> getMonthly(Long classId, Long sectionId, int year, int month) {
        List<Student> students = studentRepository
                .findAllBySchoolClassIdAndSectionIdAndDeletedFalseAndStatusOrderByRollNumberAsc(
                        classId, sectionId, StudentStatus.ACTIVE);
        students = narrowRosterToCallersScope(classId, sectionId, students);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        Map<Long, List<StudentAttendance>> byStudentId = new HashMap<>();
        if (!students.isEmpty()) {
            List<Long> studentIds = students.stream().map(Student::getId).toList();
            studentAttendanceRepository.findAllByStudentIdInAndAttendanceDateBetween(studentIds, start, end)
                    .forEach(record -> byStudentId
                            .computeIfAbsent(record.getStudent().getId(), k -> new java.util.ArrayList<>())
                            .add(record));
        }

        return students.stream()
                .map(student -> {
                    Map<String, String> days = new LinkedHashMap<>();
                    byStudentId.getOrDefault(student.getId(), List.of()).forEach(record ->
                            days.put(String.valueOf(record.getAttendanceDate().getDayOfMonth()), record.getStatus().name()));
                    return MonthlyAttendanceRowDto.builder()
                            .studentId(student.getId())
                            .firstName(firstNameOf(student))
                            .lastName(lastNameOf(student))
                            .rollNumber(student.getRollNumber())
                            .days(days)
                            .build();
                })
                .toList();
    }

    /**
     * Narrows a section's roster to the students the caller is entitled to see.
     *
     * <p>The monthly register is addressed by section, but a section is not the only
     * way to be entitled to part of one. A STUDENT or PARENT has no section rights at
     * all — {@link SectionAccessGuard} fails them closed by design — yet they are
     * plainly entitled to their own (or their children's) row of it. Requiring
     * section access outright therefore made the register a guaranteed 403 for
     * exactly the people it renders a self-view for.
     *
     * <p>So section access is now a widening grant rather than the price of entry:
     * hold it and you get the whole roster, otherwise you get the intersection of the
     * roster with your own student scope. Falling back to the scope rather than
     * returning the section wholesale is the part that matters — a student must not
     * be able to read their classmates' attendance by asking for their own section.
     *
     * <p>An empty intersection is a denial, not an empty register: it means the
     * caller asked for a section none of their students are in. A staff caller with
     * section access skips this path entirely, so a genuinely empty section still
     * renders as empty for them.
     *
     * @return the roster as the caller may see it, never empty
     * @throws AccessDeniedException if the caller may see none of it
     */
    private List<Student> narrowRosterToCallersScope(Long classId, Long sectionId, List<Student> roster) {
        if (sectionAccessGuard.canAccessSection(classId, sectionId)) {
            return roster;
        }

        List<Long> scopedIds = studentAccessGuard.resolveStudentDirectoryScope();
        // null = the caller sees every student but holds no claim on this section,
        // which no current role combination produces; treat it as no claim at all
        // rather than letting it fall through to the whole roster.
        List<Student> visible = scopedIds == null
                ? List.of()
                : roster.stream().filter(student -> scopedIds.contains(student.getId())).toList();

        if (visible.isEmpty()) {
            throw new AccessDeniedException("You are not assigned to this class/section");
        }
        return visible;
    }

    /**
     * Rejects a student who is not actually enrolled in the class/section being
     * marked. A 400 rather than a 403: the caller is entitled to mark this section,
     * they have simply sent a student who does not belong to it.
     */
    private void verifyStudentBelongsToSection(Student student, SchoolClass schoolClass, Section section) {
        boolean sameClass = student.getSchoolClass() != null
                && student.getSchoolClass().getId().equals(schoolClass.getId());
        boolean sameSection = student.getSection() != null
                && student.getSection().getId().equals(section.getId());

        if (!sameClass || !sameSection) {
            throw new BadRequestException("Student " + student.getId()
                    + " is not enrolled in the class/section being marked");
        }
    }

    /*
     * Identity comes from the student, not from the login account.
     *
     * students.user_id is nullable — a login is optional at admission — so reading
     * the name off the joined User returned null for every student admitted without
     * one, which the web client renders as "Unnamed Student" and the Android client
     * as a bare roll number. StudentMapper was moved onto the student's own columns
     * for the same reason; these three responses (grid, monthly register, report)
     * are the rest of that move.
     *
     * The account is kept only as a fallback for rows that pre-date the identity
     * backfill (database/11_student_identity.sql) or were inserted outside the app:
     * it can only ever add a name where there would otherwise be none.
     */
    private String firstNameOf(Student student) {
        if (StringUtils.hasText(student.getFirstName())) {
            return student.getFirstName();
        }
        User user = student.getUser();
        return user != null ? user.getFirstName() : null;
    }

    private String lastNameOf(Student student) {
        // Paired with the first name rather than resolved independently, so a student
        // named on their own row never picks up a stale surname from the account.
        if (StringUtils.hasText(student.getFirstName())) {
            return student.getLastName();
        }
        User user = student.getUser();
        return user != null ? user.getLastName() : null;
    }

    private long countByStatus(List<StudentAttendance> records, AttendanceStatus status) {
        return records.stream().filter(r -> r.getStatus() == status).count();
    }

    private StudentAttendanceRecordDto toRecordDto(StudentAttendance record) {
        Student student = record.getStudent();
        String markedByName = null;
        if (record.getMarkedBy() != null) {
            markedByName = userRepository.findById(record.getMarkedBy())
                    .map(u -> NameUtil.fullName(u.getFirstName(), u.getLastName()))
                    .orElse(null);
        }

        return StudentAttendanceRecordDto.builder()
                .id(record.getId())
                .studentId(student.getId())
                .firstName(firstNameOf(student))
                .lastName(lastNameOf(student))
                .admissionNumber(student.getAdmissionNumber())
                .rollNumber(student.getRollNumber())
                .classId(record.getSchoolClass().getId())
                .className(record.getSchoolClass().getClassName())
                .sectionId(record.getSection().getId())
                .sectionName(record.getSection().getSectionName())
                .attendanceDate(record.getAttendanceDate())
                .status(record.getStatus().name())
                .remarks(record.getRemarks())
                .markedBy(record.getMarkedBy())
                .markedByName(markedByName)
                .build();
    }

    private Student findEntity(Long studentId) {
        return studentRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
    }

    private SchoolClass findClass(Long id) {
        return schoolClassRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
    }

    private Section findSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", id));
    }
}

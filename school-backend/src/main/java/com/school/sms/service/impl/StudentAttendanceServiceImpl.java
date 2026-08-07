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
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentAttendanceRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.UserRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional(readOnly = true)
    public List<StudentAttendanceRowDto> getGrid(Long classId, Long sectionId, LocalDate date) {
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
                    User user = student.getUser();
                    return StudentAttendanceRowDto.builder()
                            .studentId(student.getId())
                            .firstName(user != null ? user.getFirstName() : null)
                            .lastName(user != null ? user.getLastName() : null)
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
        SchoolClass schoolClass = findClass(request.getClassId());
        Section section = findSection(request.getSectionId());
        Long markedBy = SecurityUtils.getCurrentUserId();

        int count = 0;
        for (StudentAttendanceRecordItem item : request.getRecords()) {
            Student student = studentRepository.findById(item.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", item.getStudentId()));

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
        // A scoped caller (STUDENT/PARENT) must pass their own studentId — verifyCanView
        // rejects both a missing studentId and someone else's for that caller.
        studentAccessGuard.verifyCanView(studentId);

        Specification<StudentAttendance> spec = new SpecificationBuilder<StudentAttendance>()
                .with(studentId != null, "student.id", SearchOperation.EQUALS, studentId)
                .with(classId != null, "schoolClass.id", SearchOperation.EQUALS, classId)
                .with(sectionId != null, "section.id", SearchOperation.EQUALS, sectionId)
                .with(startDate != null, "attendanceDate", SearchOperation.GREATER_THAN_EQUAL, startDate)
                .with(endDate != null, "attendanceDate", SearchOperation.LESS_THAN_EQUAL, endDate)
                .build();

        Page<StudentAttendance> page = studentAttendanceRepository.findAll(spec, pageable);
        Page<StudentAttendanceRecordDto> dtoPage = page.map(this::toRecordDto);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceSummaryDto getSummary(Long studentId, LocalDate startDate, LocalDate endDate) {
        studentAccessGuard.verifyCanView(studentId);
        findEntity(studentId);

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
    public List<MonthlyAttendanceRowDto> getMonthly(Long classId, Long sectionId, int year, int month) {
        List<Student> students = studentRepository
                .findAllBySchoolClassIdAndSectionIdAndDeletedFalseAndStatusOrderByRollNumberAsc(
                        classId, sectionId, StudentStatus.ACTIVE);

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
                    User user = student.getUser();
                    Map<String, String> days = new LinkedHashMap<>();
                    byStudentId.getOrDefault(student.getId(), List.of()).forEach(record ->
                            days.put(String.valueOf(record.getAttendanceDate().getDayOfMonth()), record.getStatus().name()));
                    return MonthlyAttendanceRowDto.builder()
                            .studentId(student.getId())
                            .firstName(user != null ? user.getFirstName() : null)
                            .lastName(user != null ? user.getLastName() : null)
                            .rollNumber(student.getRollNumber())
                            .days(days)
                            .build();
                })
                .toList();
    }

    private long countByStatus(List<StudentAttendance> records, AttendanceStatus status) {
        return records.stream().filter(r -> r.getStatus() == status).count();
    }

    private StudentAttendanceRecordDto toRecordDto(StudentAttendance record) {
        Student student = record.getStudent();
        User user = student.getUser();
        String markedByName = null;
        if (record.getMarkedBy() != null) {
            markedByName = userRepository.findById(record.getMarkedBy())
                    .map(u -> NameUtil.fullName(u.getFirstName(), u.getLastName()))
                    .orElse(null);
        }

        return StudentAttendanceRecordDto.builder()
                .id(record.getId())
                .studentId(student.getId())
                .firstName(user != null ? user.getFirstName() : null)
                .lastName(user != null ? user.getLastName() : null)
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

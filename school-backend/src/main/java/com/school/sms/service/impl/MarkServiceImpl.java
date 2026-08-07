package com.school.sms.service.impl;

import com.school.sms.dto.request.MarkEntryRequest;
import com.school.sms.dto.request.MarkRecordItem;
import com.school.sms.dto.response.MarkDto;
import com.school.sms.dto.response.MarkRosterRowDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.ReportCardDto;
import com.school.sms.dto.response.ReportCardSubjectRowDto;
import com.school.sms.entity.Exam;
import com.school.sms.entity.ExamSchedule;
import com.school.sms.entity.Grade;
import com.school.sms.entity.Mark;
import com.school.sms.entity.Student;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.ExamRepository;
import com.school.sms.repository.ExamScheduleRepository;
import com.school.sms.repository.MarkRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.security.SecurityUtils;
import com.school.sms.security.StudentAccessGuard;
import com.school.sms.service.MarkService;
import com.school.sms.util.GradeResolver;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarkServiceImpl implements MarkService {

    private final MarkRepository markRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final StudentAccessGuard studentAccessGuard;
    private final GradeResolver gradeResolver;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MarkDto> getAll(Long examScheduleId, Long studentId, Pageable pageable) {
        List<Long> ownIds = studentAccessGuard.resolveViewableStudentIds();
        if (ownIds != null && studentId != null && !ownIds.contains(studentId)) {
            throw new AccessDeniedException("You may only view your own marks");
        }

        Specification<Mark> spec = new SpecificationBuilder<Mark>()
                .with(examScheduleId != null, "examSchedule.id", SearchOperation.EQUALS, examScheduleId)
                .with(studentId != null, "student.id", SearchOperation.EQUALS, studentId)
                .with(studentId == null && ownIds != null, "student.id", SearchOperation.IN, ownIds)
                .build();

        Page<Mark> page = markRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarkRosterRowDto> getRoster(Long examScheduleId) {
        ExamSchedule schedule = examScheduleRepository.findById(examScheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("ExamSchedule", "id", examScheduleId));

        Long classId = schedule.getExam().getSchoolClass().getId();
        List<Student> students = studentRepository.findAllBySchoolClassIdAndDeletedFalseAndStatusOrderByRollNumberAsc(
                classId, com.school.sms.entity.StudentStatus.ACTIVE);

        Map<Long, Mark> markByStudentId = markRepository.findAllByExamScheduleId(examScheduleId).stream()
                .collect(Collectors.toMap(mark -> mark.getStudent().getId(), mark -> mark));

        return students.stream()
                .map(student -> {
                    Mark mark = markByStudentId.get(student.getId());
                    return MarkRosterRowDto.builder()
                            .studentId(student.getId())
                            .firstName(student.getUser() != null ? student.getUser().getFirstName() : null)
                            .lastName(student.getUser() != null ? student.getUser().getLastName() : null)
                            .rollNumber(student.getRollNumber())
                            .sectionName(student.getSection().getSectionName())
                            .marksObtained(mark != null ? mark.getMarksObtained() : null)
                            .maxMarks(schedule.getMaxMarks())
                            .gradeName(mark != null && mark.getGrade() != null ? mark.getGrade().getGradeName() : null)
                            .remarks(mark != null ? mark.getRemarks() : null)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public List<MarkDto> enterMarks(MarkEntryRequest request) {
        ExamSchedule examSchedule = examScheduleRepository.findById(request.getExamScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("ExamSchedule", "id", request.getExamScheduleId()));

        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<Mark> saved = request.getRecords().stream()
                .map(record -> saveOne(examSchedule, record, currentUserId))
                .toList();

        return saved.stream().map(this::toDto).toList();
    }

    private Mark saveOne(ExamSchedule examSchedule, MarkRecordItem record, Long currentUserId) {
        if (record.getMarksObtained().compareTo(BigDecimal.valueOf(examSchedule.getMaxMarks())) > 0) {
            throw new BadRequestException("Marks obtained cannot exceed the schedule's max marks (" + examSchedule.getMaxMarks() + ")");
        }

        Student student = studentRepository.findById(record.getStudentId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", record.getStudentId()));

        Mark mark = markRepository.findByExamScheduleIdAndStudentId(examSchedule.getId(), student.getId())
                .orElseGet(() -> Mark.builder().examSchedule(examSchedule).student(student).build());

        Grade grade = gradeResolver.resolve(record.getMarksObtained(), examSchedule.getMaxMarks());

        mark.setMarksObtained(record.getMarksObtained());
        mark.setGrade(grade);
        mark.setRemarks(record.getRemarks());
        mark.setEnteredBy(currentUserId);

        return markRepository.save(mark);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportCardDto getReportCard(Long studentId, Long examId) {
        studentAccessGuard.verifyCanView(studentId);

        Student student = studentRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", "id", examId));

        List<ExamSchedule> schedules = examScheduleRepository.findAllByExamIdOrderByExamDateAsc(examId);
        Map<Long, Mark> markByScheduleId = markRepository.findAllByStudentIdAndExamScheduleExamId(studentId, examId).stream()
                .collect(Collectors.toMap(mark -> mark.getExamSchedule().getId(), mark -> mark));

        List<ReportCardSubjectRowDto> rows = schedules.stream()
                .map(schedule -> {
                    Mark mark = markByScheduleId.get(schedule.getId());
                    return ReportCardSubjectRowDto.builder()
                            .subjectName(schedule.getSubject().getSubjectName())
                            .marksObtained(mark != null ? mark.getMarksObtained() : null)
                            .maxMarks(schedule.getMaxMarks())
                            .gradeName(mark != null && mark.getGrade() != null ? mark.getGrade().getGradeName() : null)
                            .build();
                })
                .toList();

        BigDecimal totalObtained = markByScheduleId.values().stream()
                .map(Mark::getMarksObtained)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalMax = schedules.stream().mapToInt(ExamSchedule::getMaxMarks).sum();
        BigDecimal overallPercentage = totalMax > 0
                ? totalObtained.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalMax), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Grade overallGrade = gradeResolver.resolveByPercentage(overallPercentage);

        return ReportCardDto.builder()
                .studentId(student.getId())
                .studentName(student.getUser() != null
                        ? NameUtil.fullName(student.getUser().getFirstName(), student.getUser().getLastName())
                        : null)
                .admissionNumber(student.getAdmissionNumber())
                .className(student.getSchoolClass().getClassName())
                .sectionName(student.getSection().getSectionName())
                .examId(exam.getId())
                // exams has no name column of its own per SCHEMA_CONTRACT.md — the
                // exam type's name (e.g. "Half Yearly") is used as its display name.
                .examName(exam.getExamType().getName())
                .subjects(rows)
                .totalObtained(totalObtained)
                .totalMax(totalMax)
                .overallPercentage(overallPercentage)
                .overallGrade(overallGrade != null ? overallGrade.getGradeName() : null)
                .build();
    }

    private MarkDto toDto(Mark mark) {
        Student student = mark.getStudent();
        ExamSchedule schedule = mark.getExamSchedule();
        return MarkDto.builder()
                .id(mark.getId())
                .examScheduleId(schedule.getId())
                .subjectName(schedule.getSubject().getSubjectName())
                .studentId(student.getId())
                .studentName(student.getUser() != null
                        ? NameUtil.fullName(student.getUser().getFirstName(), student.getUser().getLastName())
                        : null)
                .admissionNumber(student.getAdmissionNumber())
                .marksObtained(mark.getMarksObtained())
                .maxMarks(schedule.getMaxMarks())
                .gradeId(mark.getGrade() != null ? mark.getGrade().getId() : null)
                .gradeName(mark.getGrade() != null ? mark.getGrade().getGradeName() : null)
                .remarks(mark.getRemarks())
                .enteredBy(mark.getEnteredBy())
                .build();
    }
}

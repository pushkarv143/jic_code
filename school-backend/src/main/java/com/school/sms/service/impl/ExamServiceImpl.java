package com.school.sms.service.impl;

import com.school.sms.dto.request.ExamRequest;
import com.school.sms.dto.request.ExamScheduleRequest;
import com.school.sms.dto.response.ExamDto;
import com.school.sms.dto.response.ExamResultRowDto;
import com.school.sms.dto.response.ExamScheduleDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.AcademicYear;
import com.school.sms.entity.Exam;
import com.school.sms.entity.ExamSchedule;
import com.school.sms.entity.ExamType;
import com.school.sms.entity.Mark;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentStatus;
import com.school.sms.entity.Subject;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.repository.ExamRepository;
import com.school.sms.repository.ExamScheduleRepository;
import com.school.sms.repository.ExamTypeRepository;
import com.school.sms.repository.MarkRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.service.ExamService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamTypeRepository examTypeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final SubjectRepository subjectRepository;
    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExamDto> getAll(Long classId, Long academicYearId, Long examTypeId, Pageable pageable) {
        Specification<Exam> spec = new SpecificationBuilder<Exam>()
                .with(classId != null, "schoolClass.id", SearchOperation.EQUALS, classId)
                .with(academicYearId != null, "academicYear.id", SearchOperation.EQUALS, academicYearId)
                .with(examTypeId != null, "examType.id", SearchOperation.EQUALS, examTypeId)
                .build();

        Page<Exam> page = examRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExamDto getById(Long id) {
        return toDto(findEntity(id));
    }

    @Override
    @Transactional
    public ExamDto create(ExamRequest request) {
        ExamType examType = findExamType(request.getExamTypeId());
        SchoolClass schoolClass = findClass(request.getClassId());
        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());

        Exam exam = Exam.builder()
                .examType(examType)
                .schoolClass(schoolClass)
                .academicYear(academicYear)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        return toDto(examRepository.save(exam));
    }

    @Override
    @Transactional
    public ExamDto update(Long id, ExamRequest request) {
        Exam exam = findEntity(id);
        exam.setExamType(findExamType(request.getExamTypeId()));
        exam.setSchoolClass(findClass(request.getClassId()));
        exam.setAcademicYear(findAcademicYear(request.getAcademicYearId()));
        exam.setStartDate(request.getStartDate());
        exam.setEndDate(request.getEndDate());
        return toDto(examRepository.save(exam));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // exam_schedules/marks referencing this exam cascade-delete at the
        // database level (fk_es_exam / fk_marks_schedule ON DELETE CASCADE).
        examRepository.delete(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamScheduleDto> getSchedules(Long examId) {
        findEntity(examId);
        return examScheduleRepository.findAllByExamIdOrderByExamDateAsc(examId).stream()
                .map(this::toScheduleDto)
                .toList();
    }

    @Override
    @Transactional
    public ExamScheduleDto addSchedule(Long examId, ExamScheduleRequest request) {
        Exam exam = findEntity(examId);
        Subject subject = findSubject(request.getSubjectId());

        if (examScheduleRepository.existsByExamIdAndSubjectId(examId, subject.getId())) {
            throw new BadRequestException("A schedule for this subject already exists for this exam");
        }
        if (request.getExamDate() != null && request.getRoomNumber() != null
                && examScheduleRepository.existsByExamIdAndExamDateAndRoomNumber(examId, request.getExamDate(), request.getRoomNumber())) {
            throw new BadRequestException("Another schedule is already booked for this room on this date");
        }

        ExamSchedule schedule = ExamSchedule.builder()
                .exam(exam)
                .subject(subject)
                .examDate(request.getExamDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxMarks(request.getMaxMarks())
                .roomNumber(request.getRoomNumber())
                .build();

        return toScheduleDto(examScheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public ExamScheduleDto updateSchedule(Long scheduleId, ExamScheduleRequest request) {
        ExamSchedule schedule = findSchedule(scheduleId);
        Long examId = schedule.getExam().getId();
        Subject subject = findSubject(request.getSubjectId());

        if (examScheduleRepository.existsByExamIdAndSubjectIdAndIdNot(examId, subject.getId(), scheduleId)) {
            throw new BadRequestException("A schedule for this subject already exists for this exam");
        }
        if (request.getExamDate() != null && request.getRoomNumber() != null
                && examScheduleRepository.existsByExamIdAndExamDateAndRoomNumberAndIdNot(
                        examId, request.getExamDate(), request.getRoomNumber(), scheduleId)) {
            throw new BadRequestException("Another schedule is already booked for this room on this date");
        }

        schedule.setSubject(subject);
        schedule.setExamDate(request.getExamDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setMaxMarks(request.getMaxMarks());
        schedule.setRoomNumber(request.getRoomNumber());

        return toScheduleDto(examScheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public void deleteSchedule(Long scheduleId) {
        // marks referencing this schedule cascade-delete at the database
        // level (fk_marks_schedule ON DELETE CASCADE).
        examScheduleRepository.delete(findSchedule(scheduleId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamResultRowDto> getResults(Long examId, Long classId, Long sectionId) {
        Exam exam = findEntity(examId);
        Long effectiveClassId = classId != null ? classId : exam.getSchoolClass().getId();

        List<Student> students = sectionId != null
                ? studentRepository.findAllBySchoolClassIdAndSectionIdAndDeletedFalseAndStatusOrderByRollNumberAsc(
                        effectiveClassId, sectionId, StudentStatus.ACTIVE)
                : studentRepository.findAllBySchoolClassIdAndDeletedFalseAndStatusOrderByRollNumberAsc(
                        effectiveClassId, StudentStatus.ACTIVE);

        List<ExamSchedule> schedules = examScheduleRepository.findAllByExamIdOrderByExamDateAsc(examId);
        int totalMax = schedules.stream().mapToInt(ExamSchedule::getMaxMarks).sum();

        Map<Long, BigDecimal> obtainedByStudent = markRepository.findAllByExamScheduleExamId(examId).stream()
                .collect(Collectors.groupingBy(
                        mark -> mark.getStudent().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Mark::getMarksObtained, BigDecimal::add)));

        List<ExamResultRowDto> rows = students.stream()
                .map(student -> {
                    BigDecimal obtained = obtainedByStudent.getOrDefault(student.getId(), BigDecimal.ZERO);
                    BigDecimal percentage = totalMax > 0
                            ? obtained.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalMax), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return ExamResultRowDto.builder()
                            .studentId(student.getId())
                            .studentName(student.getUser() != null
                                    ? NameUtil.fullName(student.getUser().getFirstName(), student.getUser().getLastName())
                                    : null)
                            .rollNumber(student.getRollNumber())
                            .totalObtained(obtained)
                            .totalMax(totalMax)
                            .percentage(percentage)
                            .build();
                })
                .sorted(Comparator.comparing(ExamResultRowDto::getPercentage).reversed())
                .collect(Collectors.toList());

        // Dense ranking: ties share a rank, the next distinct percentage takes the following rank.
        int rank = 0;
        BigDecimal previousPercentage = null;
        for (ExamResultRowDto row : rows) {
            if (previousPercentage == null || row.getPercentage().compareTo(previousPercentage) != 0) {
                rank++;
            }
            row.setRank(rank);
            previousPercentage = row.getPercentage();
        }

        return rows;
    }

    private ExamDto toDto(Exam exam) {
        return ExamDto.builder()
                .id(exam.getId())
                .examTypeId(exam.getExamType().getId())
                .examTypeName(exam.getExamType().getName())
                .classId(exam.getSchoolClass().getId())
                .className(exam.getSchoolClass().getClassName())
                .academicYearId(exam.getAcademicYear().getId())
                .academicYearName(exam.getAcademicYear().getYearName())
                .startDate(exam.getStartDate())
                .endDate(exam.getEndDate())
                .build();
    }

    private ExamScheduleDto toScheduleDto(ExamSchedule schedule) {
        return ExamScheduleDto.builder()
                .id(schedule.getId())
                .examId(schedule.getExam().getId())
                .subjectId(schedule.getSubject().getId())
                .subjectName(schedule.getSubject().getSubjectName())
                .examDate(schedule.getExamDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .maxMarks(schedule.getMaxMarks())
                .roomNumber(schedule.getRoomNumber())
                .build();
    }

    private Exam findEntity(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", "id", id));
    }

    private ExamSchedule findSchedule(Long id) {
        return examScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExamSchedule", "id", id));
    }

    private ExamType findExamType(Long id) {
        return examTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExamType", "id", id));
    }

    private SchoolClass findClass(Long id) {
        return schoolClassRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
    }

    private AcademicYear findAcademicYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }

    private Subject findSubject(Long id) {
        return subjectRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));
    }
}

package com.school.sms.service.impl;

import com.school.sms.dto.request.GenerateStudentFeesRequest;
import com.school.sms.dto.response.FeePaymentDto;
import com.school.sms.dto.response.FeesDuesSummaryDto;
import com.school.sms.dto.response.GenerateFeesResultDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentFeeDto;
import com.school.sms.entity.AcademicYear;
import com.school.sms.entity.FeePayment;
import com.school.sms.entity.FeeStatus;
import com.school.sms.entity.FeeStructure;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentFee;
import com.school.sms.entity.StudentStatus;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.FeePaymentMapper;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.repository.FeeStructureRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.StudentFeeRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.StudentAccessGuard;
import com.school.sms.service.StudentFeeService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentFeeServiceImpl implements StudentFeeService {

    private final StudentFeeRepository studentFeeRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AcademicYearRepository academicYearRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final UserRepository userRepository;
    private final FeePaymentMapper feePaymentMapper;
    private final StudentAccessGuard studentAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentFeeDto> getAll(Long studentId, Long classId, Long sectionId, String status,
                                               Long academicYearId, Pageable pageable) {
        List<Long> ownIds = studentAccessGuard.resolveViewableStudentIds();
        if (ownIds != null && studentId != null && !ownIds.contains(studentId)) {
            throw new org.springframework.security.access.AccessDeniedException("You may only view your own fee records");
        }

        Specification<StudentFee> spec = new SpecificationBuilder<StudentFee>()
                .with(studentId != null, "student.id", SearchOperation.EQUALS, studentId)
                .with(studentId == null && ownIds != null, "student.id", SearchOperation.IN, ownIds)
                .with(classId != null, "student.schoolClass.id", SearchOperation.EQUALS, classId)
                .with(sectionId != null, "student.section.id", SearchOperation.EQUALS, sectionId)
                .with(academicYearId != null, "academicYear.id", SearchOperation.EQUALS, academicYearId)
                .with(StringUtils.hasText(status), "status", SearchOperation.EQUALS,
                        StringUtils.hasText(status) ? FeeStatus.valueOf(status.toUpperCase()) : null)
                .build();

        Page<StudentFee> page = studentFeeRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(fee -> toDto(fee, false)).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentFeeDto getById(Long id) {
        StudentFee fee = findEntity(id);
        studentAccessGuard.verifyCanView(fee.getStudent().getId());
        return toDto(fee, true);
    }

    @Override
    @Transactional
    public GenerateFeesResultDto generate(GenerateStudentFeesRequest request) {
        findClass(request.getClassId());
        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());

        List<FeeStructure> feeStructures = feeStructureRepository.findAllByIdInAndSchoolClassIdAndAcademicYearId(
                request.getFeeStructureIds(), request.getClassId(), request.getAcademicYearId());
        if (feeStructures.size() != request.getFeeStructureIds().stream().distinct().count()) {
            throw new BadRequestException(
                    "One or more fee structures do not belong to the given class and academic year");
        }

        List<Student> students = studentRepository.findAllBySchoolClassIdAndDeletedFalseAndStatusOrderByRollNumberAsc(
                request.getClassId(), StudentStatus.ACTIVE);

        int generated = 0;
        int skipped = 0;
        List<StudentFee> toSave = new ArrayList<>();
        for (Student student : students) {
            for (FeeStructure feeStructure : feeStructures) {
                if (studentFeeRepository.existsByStudentIdAndFeeStructureId(student.getId(), feeStructure.getId())) {
                    skipped++;
                    continue;
                }
                toSave.add(StudentFee.builder()
                        .student(student)
                        .feeStructure(feeStructure)
                        .academicYear(academicYear)
                        .amountDue(feeStructure.getAmount())
                        .amountPaid(BigDecimal.ZERO)
                        .dueDate(feeStructure.getDueDate())
                        .status(FeeStatus.UNPAID)
                        .build());
                generated++;
            }
        }
        studentFeeRepository.saveAll(toSave);

        return GenerateFeesResultDto.builder().generatedCount(generated).skippedCount(skipped).build();
    }

    @Override
    @Transactional(readOnly = true)
    public FeesDuesSummaryDto getDuesSummary(Long classId, Long academicYearId) {
        List<Object[]> rows = studentFeeRepository.aggregateDuesSummary(classId, academicYearId);
        Object[] row = rows.isEmpty() ? new Object[]{BigDecimal.ZERO, BigDecimal.ZERO, 0L} : rows.get(0);

        BigDecimal totalDue = (BigDecimal) row[0];
        BigDecimal totalCollected = (BigDecimal) row[1];
        long studentCount = ((Number) row[2]).longValue();

        return FeesDuesSummaryDto.builder()
                .totalDue(totalDue)
                .totalCollected(totalCollected)
                .totalOutstanding(totalDue.subtract(totalCollected))
                .studentCount(studentCount)
                .build();
    }

    StudentFeeDto toDto(StudentFee fee, boolean includePayments) {
        Student student = fee.getStudent();
        User user = student.getUser();
        String studentName = user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null;

        StudentFeeDto.StudentFeeDtoBuilder builder = StudentFeeDto.builder()
                .id(fee.getId())
                .studentId(student.getId())
                .studentName(studentName)
                .admissionNumber(student.getAdmissionNumber())
                .classId(student.getSchoolClass().getId())
                .className(student.getSchoolClass().getClassName())
                .sectionId(student.getSection().getId())
                .sectionName(student.getSection().getSectionName())
                .feeStructureId(fee.getFeeStructure().getId())
                .feeCategoryId(fee.getFeeStructure().getFeeCategory().getId())
                .feeCategoryName(fee.getFeeStructure().getFeeCategory().getName())
                .academicYearId(fee.getAcademicYear().getId())
                .academicYearName(fee.getAcademicYear().getYearName())
                .amountDue(fee.getAmountDue())
                .amountPaid(fee.getAmountPaid())
                .dueDate(fee.getDueDate())
                .status(fee.getStatus().name());

        if (includePayments) {
            List<FeePaymentDto> payments = fee.getFeePayments().stream()
                    .map(payment -> toPaymentDto(payment, student.getId(), studentName))
                    .toList();
            builder.feePayments(payments);
        }

        return builder.build();
    }

    private FeePaymentDto toPaymentDto(FeePayment payment, Long studentId, String studentName) {
        FeePaymentDto dto = feePaymentMapper.toDto(payment);
        dto.setStudentId(studentId);
        dto.setStudentName(studentName);
        if (payment.getCollectedBy() != null) {
            userRepository.findById(payment.getCollectedBy())
                    .ifPresent(u -> dto.setCollectedByName(NameUtil.fullName(u.getFirstName(), u.getLastName())));
        }
        return dto;
    }

    private StudentFee findEntity(Long id) {
        return studentFeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudentFee", "id", id));
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
}

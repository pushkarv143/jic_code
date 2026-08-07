package com.school.sms.service.impl;

import com.school.sms.dto.request.FeePaymentRequest;
import com.school.sms.dto.response.FeePaymentDto;
import com.school.sms.dto.response.FeePaymentResultDto;
import com.school.sms.dto.response.FeeReceiptDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentFeeDto;
import com.school.sms.entity.FeePayment;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentFee;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.FeePaymentMapper;
import com.school.sms.repository.FeePaymentRepository;
import com.school.sms.repository.StudentFeeRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.SecurityUtils;
import com.school.sms.security.StudentAccessGuard;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.FeePaymentService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeePaymentServiceImpl implements FeePaymentService {

    private static final String DEFAULT_SCHOOL_NAME = "School Management System";

    private final FeePaymentRepository feePaymentRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final UserRepository userRepository;
    private final FeePaymentMapper feePaymentMapper;
    private final StudentAccessGuard studentAccessGuard;
    private final AuditLogService auditLogService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public FeePaymentResultDto pay(FeePaymentRequest request) {
        StudentFee studentFee = studentFeeRepository.findById(request.getStudentFeeId())
                .orElseThrow(() -> new ResourceNotFoundException("StudentFee", "id", request.getStudentFeeId()));

        BigDecimal outstanding = studentFee.getAmountDue().subtract(studentFee.getAmountPaid());
        if (request.getAmount().compareTo(outstanding) > 0) {
            throw new BadRequestException(String.format(
                    "Payment amount (%s) exceeds the outstanding balance (%s) for this fee",
                    request.getAmount(), outstanding));
        }

        FeePayment payment = FeePayment.builder()
                .studentFee(studentFee)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .paymentMode(request.getPaymentMode())
                .transactionId(request.getTransactionId())
                .receiptNumber(generateReceiptNumber())
                .collectedBy(SecurityUtils.getCurrentUserId())
                .build();

        // saveAndFlush forces the INSERT to execute now, so the
        // trg_fee_payments_after_insert trigger (see database/04_triggers.sql)
        // has already recomputed student_fees.amount_paid/status by the time
        // we refresh below. Hibernate's persistence context would otherwise
        // keep returning the stale, already-loaded studentFee instance.
        FeePayment saved = feePaymentRepository.saveAndFlush(payment);
        entityManager.refresh(studentFee);
        auditLogService.record("FEE_PAYMENT", "FeePayment", saved.getId(), null, null);

        FeePaymentDto paymentDto = enrichPayment(feePaymentMapper.toDto(saved), studentFee.getStudent());
        StudentFeeDto studentFeeDto = toStudentFeeDto(studentFee);

        return FeePaymentResultDto.builder()
                .payment(paymentDto)
                .updatedStudentFee(studentFeeDto)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FeePaymentDto> getAll(Long studentId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        List<Long> ownIds = studentAccessGuard.resolveViewableStudentIds();
        if (ownIds != null && studentId != null && !ownIds.contains(studentId)) {
            throw new org.springframework.security.access.AccessDeniedException("You may only view your own payment history");
        }

        Specification<FeePayment> spec = new SpecificationBuilder<FeePayment>()
                .with(studentId != null, "studentFee.student.id", SearchOperation.EQUALS, studentId)
                .with(studentId == null && ownIds != null, "studentFee.student.id", SearchOperation.IN, ownIds)
                .with(startDate != null, "paymentDate", SearchOperation.GREATER_THAN_EQUAL, startDate)
                .with(endDate != null, "paymentDate", SearchOperation.LESS_THAN_EQUAL, endDate)
                .build();

        Page<FeePayment> page = feePaymentRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream()
                .map(payment -> enrichPayment(feePaymentMapper.toDto(payment), payment.getStudentFee().getStudent()))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FeeReceiptDto getReceipt(Long paymentId) {
        FeePayment payment = feePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("FeePayment", "id", paymentId));

        Student student = payment.getStudentFee().getStudent();
        studentAccessGuard.verifyCanView(student.getId());

        User user = student.getUser();
        String collectedByName = null;
        if (payment.getCollectedBy() != null) {
            collectedByName = userRepository.findById(payment.getCollectedBy())
                    .map(u -> NameUtil.fullName(u.getFirstName(), u.getLastName()))
                    .orElse(null);
        }

        return FeeReceiptDto.builder()
                .receiptNumber(payment.getReceiptNumber())
                .studentName(user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null)
                .admissionNumber(student.getAdmissionNumber())
                .className(student.getSchoolClass().getClassName())
                .sectionName(student.getSection().getSectionName())
                .feeCategoryName(payment.getStudentFee().getFeeStructure().getFeeCategory().getName())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMode(payment.getPaymentMode().name())
                .collectedByName(collectedByName)
                .schoolName(DEFAULT_SCHOOL_NAME)
                .transactionId(payment.getTransactionId())
                .academicYearName(payment.getStudentFee().getAcademicYear().getYearName())
                .build();
    }

    private String generateReceiptNumber() {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_generate_fee_receipt_number");
        query.registerStoredProcedureParameter("p_receipt_number", String.class, ParameterMode.OUT);
        query.execute();
        return (String) query.getOutputParameterValue("p_receipt_number");
    }

    private FeePaymentDto enrichPayment(FeePaymentDto dto, Student student) {
        User user = student.getUser();
        dto.setStudentId(student.getId());
        dto.setStudentName(user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null);
        if (dto.getCollectedBy() != null) {
            userRepository.findById(dto.getCollectedBy())
                    .ifPresent(u -> dto.setCollectedByName(NameUtil.fullName(u.getFirstName(), u.getLastName())));
        }
        return dto;
    }

    private StudentFeeDto toStudentFeeDto(StudentFee fee) {
        Student student = fee.getStudent();
        User user = student.getUser();
        return StudentFeeDto.builder()
                .id(fee.getId())
                .studentId(student.getId())
                .studentName(user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null)
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
                .status(fee.getStatus().name())
                .build();
    }
}

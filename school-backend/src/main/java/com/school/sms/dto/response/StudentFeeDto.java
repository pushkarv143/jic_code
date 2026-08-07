package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFeeDto {

    private Long id;

    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;

    private Long feeStructureId;
    private Long feeCategoryId;
    private String feeCategoryName;

    private Long academicYearId;
    private String academicYearName;

    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private LocalDate dueDate;
    private String status;

    // Populated only on the single-resource GET.
    private List<FeePaymentDto> feePayments;
}

package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Structured, print-ready shape for a fee payment receipt. Rendering an
 * actual PDF is out of scope for this round (see the module README note on
 * FeePaymentServiceImpl) — the frontend renders this JSON.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeReceiptDto {

    private String receiptNumber;
    private String studentName;
    private String admissionNumber;
    private String className;
    private String sectionName;
    private String feeCategoryName;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMode;
    private String collectedByName;
    private String schoolName;
    private String transactionId;
    private String academicYearName;
}

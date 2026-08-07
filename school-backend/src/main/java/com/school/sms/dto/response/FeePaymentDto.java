package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeePaymentDto {

    private Long id;
    private Long studentFeeId;
    private Long studentId;
    private String studentName;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMode;
    private String transactionId;
    private String receiptNumber;
    private Long collectedBy;
    private String collectedByName;
}

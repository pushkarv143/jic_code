package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostelFeeDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private Integer month;
    private Integer year;
    private BigDecimal amount;
    private String paidStatus;
}

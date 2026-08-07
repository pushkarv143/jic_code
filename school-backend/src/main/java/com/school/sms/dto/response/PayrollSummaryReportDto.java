package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollSummaryReportDto {

    private BigDecimal totalPaidAmount;
    private BigDecimal totalPendingAmount;
    private List<MonthPaidDto> byMonth;
}

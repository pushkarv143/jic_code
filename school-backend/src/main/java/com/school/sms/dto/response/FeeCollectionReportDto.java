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
public class FeeCollectionReportDto {

    private BigDecimal totalDue;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    private List<CategoryAmountDto> byCategory;
    private List<MonthCollectedDto> byMonth;
}

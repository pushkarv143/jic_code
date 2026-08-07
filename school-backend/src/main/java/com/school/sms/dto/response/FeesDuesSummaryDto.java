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
public class FeesDuesSummaryDto {

    private BigDecimal totalDue;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    private long studentCount;
}

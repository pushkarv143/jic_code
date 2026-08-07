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
public class SalaryStructureDto {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeType;
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal da;
    private BigDecimal otherAllowances;
    private BigDecimal pfPercentage;
    private BigDecimal esiPercentage;
}

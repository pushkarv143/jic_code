package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Structured, print-ready shape for a payroll salary slip — same
 * "printable JSON, no PDF yet" pattern as {@link FeeReceiptDto}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalarySlipDto {

    private String employeeName;
    private String employeeId;
    private String departmentName;
    private String designationName;
    private Integer month;
    private Integer year;
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal da;
    private BigDecimal otherAllowances;
    private BigDecimal pf;
    private BigDecimal esi;
    private BigDecimal netSalary;
    private String schoolName;
}

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
public class PayrollDto {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeType;
    private Integer month;
    private Integer year;
    private BigDecimal basicSalary;
    private BigDecimal allowances;
    private BigDecimal deductions;
    private BigDecimal pf;
    private BigDecimal esi;
    private BigDecimal netSalary;
    private LocalDate paymentDate;
    private String status;
}

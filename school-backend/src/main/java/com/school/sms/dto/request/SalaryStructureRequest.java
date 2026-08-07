package com.school.sms.dto.request;

import com.school.sms.entity.PayrollEmployeeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaryStructureRequest {

    @NotNull(message = "Employee id is required")
    private Long employeeId;

    @NotNull(message = "Employee type is required")
    private PayrollEmployeeType employeeType;

    @NotNull(message = "Basic salary is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Basic salary must not be negative")
    private BigDecimal basicSalary;

    @NotNull(message = "HRA is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "HRA must not be negative")
    private BigDecimal hra;

    @NotNull(message = "DA is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "DA must not be negative")
    private BigDecimal da;

    @NotNull(message = "Other allowances is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Other allowances must not be negative")
    private BigDecimal otherAllowances;

    @NotNull(message = "PF percentage is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "PF percentage must not be negative")
    private BigDecimal pfPercentage;

    @NotNull(message = "ESI percentage is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "ESI percentage must not be negative")
    private BigDecimal esiPercentage;
}

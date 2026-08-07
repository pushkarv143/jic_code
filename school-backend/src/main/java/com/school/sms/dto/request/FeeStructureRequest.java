package com.school.sms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructureRequest {

    @NotNull(message = "Class is required")
    private Long classId;

    @NotNull(message = "Academic year is required")
    private Long academicYearId;

    @NotNull(message = "Fee category is required")
    private Long feeCategoryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private LocalDate dueDate;
}

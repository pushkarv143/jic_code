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
public class FeeStructureDto {

    private Long id;
    private Long classId;
    private String className;
    private Long academicYearId;
    private String academicYearName;
    private Long feeCategoryId;
    private String feeCategoryName;
    private BigDecimal amount;
    private LocalDate dueDate;
}

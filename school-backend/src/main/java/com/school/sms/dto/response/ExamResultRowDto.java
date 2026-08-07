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
public class ExamResultRowDto {

    private Long studentId;
    private String studentName;
    private Integer rollNumber;
    private BigDecimal totalObtained;
    private Integer totalMax;
    private BigDecimal percentage;
    private int rank;
}

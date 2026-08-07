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
public class ScholarshipDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String title;
    private BigDecimal amount;
    private String type;
    private Long academicYearId;
    private String academicYearName;
    private Long approvedBy;
    private String approvedByName;
}

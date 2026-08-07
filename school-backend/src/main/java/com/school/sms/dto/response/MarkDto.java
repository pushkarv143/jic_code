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
public class MarkDto {

    private Long id;
    private Long examScheduleId;
    private String subjectName;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private BigDecimal marksObtained;
    private Integer maxMarks;
    private Long gradeId;
    private String gradeName;
    private String remarks;
    private Long enteredBy;
}

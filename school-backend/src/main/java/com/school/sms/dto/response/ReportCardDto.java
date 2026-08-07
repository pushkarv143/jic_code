package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCardDto {

    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String className;
    private String sectionName;
    private Long examId;
    private String examName;
    private List<ReportCardSubjectRowDto> subjects;
    private BigDecimal totalObtained;
    private Integer totalMax;
    private BigDecimal overallPercentage;
    private String overallGrade;
}

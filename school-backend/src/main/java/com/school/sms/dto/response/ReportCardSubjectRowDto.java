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
public class ReportCardSubjectRowDto {

    private String subjectName;
    private BigDecimal marksObtained;
    private Integer maxMarks;
    private String gradeName;
}

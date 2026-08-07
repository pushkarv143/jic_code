package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamDto {

    private Long id;
    private Long examTypeId;
    private String examTypeName;
    private Long classId;
    private String className;
    private Long academicYearId;
    private String academicYearName;
    private LocalDate startDate;
    private LocalDate endDate;
}

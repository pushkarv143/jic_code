package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolClassDto {

    private Long id;
    private String className;
    private Long academicYearId;
    private String academicYearName;
    private int sectionCount;
    private int subjectCount;
}

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
public class SectionDto {

    private Long id;
    private String sectionName;
    private Long classId;
    private String className;
    private Long classTeacherId;
    private String classTeacherName;
    private String roomNumber;
    private Integer capacity;
    /** Active students enrolled in this section. */
    private Integer studentCount;
}

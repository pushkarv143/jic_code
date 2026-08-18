package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Where a teacher's homeroom already is, if they have one.
 *
 * <p>Exists so the class screen can grey out a teacher who is already taken
 * instead of offering them and failing the save. Returned for assigned teachers
 * only — anyone absent from the list is free.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassTeacherAvailabilityDto {

    private Long teacherId;
    private String teacherName;
    private Long sectionId;
    private String sectionName;
    private Long classId;
    private String className;
}

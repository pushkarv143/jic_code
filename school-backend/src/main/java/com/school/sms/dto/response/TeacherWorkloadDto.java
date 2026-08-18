package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * How much one teacher is carrying: distinct subject/section mappings, the
 * sections they are homeroom for, and timetabled periods per week.
 *
 * <p>Mappings and periods are counted separately because they answer different
 * questions — a teacher can be mapped to a subject that has not been timetabled
 * yet, and a timetabled period need not have a mapping behind it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherWorkloadDto {

    private Long teacherId;
    private String teacherName;
    private String employeeId;
    private long subjectMappings;
    private long sectionsTaught;
    private long classTeacherOf;
    private long weeklyPeriods;
}

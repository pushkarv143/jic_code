package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/** One period. subject/teacher are null for a non-teaching slot named by {@code label}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableSlotDto {

    private Long id;
    private Long classId;
    /**
     * Carried alongside {@code classId} because a teacher's own week spans several
     * classes: without the name every period would read as a bare id to the one
     * person the grid is for.
     */
    private String className;
    private Long sectionId;
    private String sectionName;
    private String dayOfWeek;
    private Integer periodNumber;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long subjectId;
    private String subjectName;
    private Long teacherId;
    private String teacherName;
    private String roomNumber;
    private String label;
    /** Set when this slot double-books its teacher or its room. */
    private String clashWarning;
}

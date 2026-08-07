package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row of the "mark attendance" grid for a class/section/date — status
 * and remarks are null when the student has not yet been marked that day.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAttendanceRowDto {

    private Long studentId;
    private String firstName;
    private String lastName;
    private Integer rollNumber;
    private String status;
    private String remarks;
}

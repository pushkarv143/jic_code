package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * One row of the teacher attendance grid for a given date — status/checkIn/
 * checkOut/remarks are null when the teacher has not yet been marked that day.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAttendanceRowDto {

    private Long teacherId;
    private String firstName;
    private String lastName;
    private String employeeId;
    private String status;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private String remarks;
}

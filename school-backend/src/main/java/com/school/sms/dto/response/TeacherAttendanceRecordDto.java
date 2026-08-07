package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAttendanceRecordDto {

    private Long id;
    private Long teacherId;
    private String firstName;
    private String lastName;
    private String employeeId;
    private LocalDate attendanceDate;
    private String status;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private String remarks;
}

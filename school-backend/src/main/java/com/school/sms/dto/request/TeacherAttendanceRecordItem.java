package com.school.sms.dto.request;

import com.school.sms.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAttendanceRecordItem {

    @NotNull(message = "Teacher is required")
    private Long teacherId;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    private LocalTime checkIn;

    private LocalTime checkOut;

    private String remarks;
}

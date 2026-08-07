package com.school.sms.dto.request;

import com.school.sms.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row of a {@link MarkStudentAttendanceRequest#getRecords()} batch.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceRecordItem {

    @NotNull(message = "Student is required")
    private Long studentId;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    private String remarks;
}

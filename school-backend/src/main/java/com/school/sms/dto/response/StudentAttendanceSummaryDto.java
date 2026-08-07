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
public class StudentAttendanceSummaryDto {

    private long presentDays;
    private long absentDays;
    private long lateDays;
    private long halfDays;
    private long leaveDays;
    private long totalMarkedDays;
    private double percentage;
}

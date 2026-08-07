package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * One row per student for the monthly attendance grid: {@code days} is keyed
 * by day-of-month as a string ("1", "2", ...) and only contains entries for
 * days that actually have an attendance record.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyAttendanceRowDto {

    private Long studentId;
    private String firstName;
    private String lastName;
    private Integer rollNumber;
    private Map<String, String> days;
}

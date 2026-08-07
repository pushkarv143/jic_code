package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * School-wide (or single-class, when classId is given) attendance aggregate.
 * Distinct from round 3's per-student StudentAttendanceSummaryDto.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSummaryReportDto {

    private double averagePercentage;
    private List<ClassPercentageDto> byClass;
}

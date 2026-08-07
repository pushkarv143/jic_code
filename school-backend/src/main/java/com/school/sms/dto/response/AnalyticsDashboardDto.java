package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Combined headline-numbers DTO for the management analytics landing page.
 * Composed in AnalyticsServiceImpl from ReportService's own methods (student/
 * teacher/fee/attendance totals) rather than re-querying, to keep a single
 * source of truth for each figure.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsDashboardDto {

    private long totalActiveStudents;
    private long totalActiveTeachers;
    private BigDecimal totalFeeCollected;
    private BigDecimal totalFeeOutstanding;
    private double averageAttendancePercentage;
}

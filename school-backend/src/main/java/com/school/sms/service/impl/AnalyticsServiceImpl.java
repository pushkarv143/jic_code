package com.school.sms.service.impl;

import com.school.sms.dto.response.AnalyticsDashboardDto;
import com.school.sms.dto.response.AttendanceSummaryReportDto;
import com.school.sms.dto.response.FeeCollectionReportDto;
import com.school.sms.dto.response.StudentsSummaryDto;
import com.school.sms.dto.response.TeachersSummaryDto;
import com.school.sms.service.AnalyticsService;
import com.school.sms.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Composes the management analytics landing page from ReportService's own
 * methods (student/teacher/fee/attendance totals) rather than re-querying the
 * repositories directly, so each headline figure has exactly one source of
 * truth (see ReportServiceImpl).
 */
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ReportService reportService;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsDashboardDto getDashboard() {
        StudentsSummaryDto students = reportService.getStudentsSummary();
        TeachersSummaryDto teachers = reportService.getTeachersSummary();
        FeeCollectionReportDto fees = reportService.getFeeCollectionReport(null);

        // No date range is meaningful for a landing-page headline number, so this
        // defaults to "so far this calendar month" — the frontend can call
        // /api/v1/reports/attendance-summary directly for a custom range.
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        AttendanceSummaryReportDto attendance = reportService.getAttendanceSummary(monthStart, today, null);

        return AnalyticsDashboardDto.builder()
                .totalActiveStudents(students.getTotalActive())
                .totalActiveTeachers(teachers.getTotalActive())
                .totalFeeCollected(fees.getTotalCollected())
                .totalFeeOutstanding(fees.getTotalOutstanding())
                .averageAttendancePercentage(attendance.getAveragePercentage())
                .build();
    }
}

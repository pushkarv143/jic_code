package com.greenwood.school.data.remote.dto

import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Reporting + analytics. These back both the Reports screen and every        */
/* role's dashboard.                                                          */
/* ------------------------------------------------------------------------- */

@Serializable
data class ClassCountBreakdownDto(val className: String = "", val count: Long = 0)

@Serializable
data class StatusCountBreakdownDto(val status: String = "", val count: Long = 0)

@Serializable
data class DepartmentCountBreakdownDto(val departmentName: String = "", val count: Long = 0)

@Serializable
data class ClassPercentageBreakdownDto(val className: String = "", val percentage: Double = 0.0)

@Serializable
data class CategoryCollectedBreakdownDto(val categoryName: String = "", val collected: Double = 0.0)

@Serializable
data class CategoryCountBreakdownDto(val categoryName: String = "", val count: Long = 0)

/** `month` comes back as either a number or a name depending on the report. */
@Serializable
data class MonthCollectedBreakdownDto(val month: String = "", val collected: Double = 0.0)

@Serializable
data class MonthPaidBreakdownDto(val month: String = "", val paidAmount: Double = 0.0)

@Serializable
data class StudentsSummaryReportDto(
    val totalActive: Long = 0,
    val byClass: List<ClassCountBreakdownDto> = emptyList(),
    val byStatus: List<StatusCountBreakdownDto> = emptyList(),
)

@Serializable
data class TeachersSummaryReportDto(
    val totalActive: Long = 0,
    val byDepartment: List<DepartmentCountBreakdownDto> = emptyList(),
)

@Serializable
data class AttendanceSummaryReportDto(
    val averagePercentage: Double = 0.0,
    val byClass: List<ClassPercentageBreakdownDto> = emptyList(),
)

@Serializable
data class FeeCollectionReportDto(
    val totalDue: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val byCategory: List<CategoryCollectedBreakdownDto> = emptyList(),
    val byMonth: List<MonthCollectedBreakdownDto> = emptyList(),
)

@Serializable
data class PayrollSummaryReportDto(
    val totalPaidAmount: Double = 0.0,
    val totalPendingAmount: Double = 0.0,
    val byMonth: List<MonthPaidBreakdownDto> = emptyList(),
)

@Serializable
data class LibrarySummaryReportDto(
    val totalBooks: Long = 0,
    val totalIssued: Long = 0,
    val totalOverdue: Long = 0,
    val byCategory: List<CategoryCountBreakdownDto> = emptyList(),
)

@Serializable
data class TransportSummaryReportDto(
    val totalBuses: Long = 0,
    val totalRoutes: Long = 0,
    val studentsUsingTransport: Long = 0,
)

/** `GET /analytics/dashboard` — the five headline numbers on the admin dashboard. */
@Serializable
data class AnalyticsDashboardDto(
    val totalActiveStudents: Long = 0,
    val totalActiveTeachers: Long = 0,
    val totalFeeCollected: Double = 0.0,
    val totalFeeOutstanding: Double = 0.0,
    val averageAttendancePercentage: Double = 0.0,
)

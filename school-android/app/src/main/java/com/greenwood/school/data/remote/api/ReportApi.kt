package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.AnalyticsDashboardDto
import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.AttendanceSummaryReportDto
import com.greenwood.school.data.remote.dto.FeeCollectionReportDto
import com.greenwood.school.data.remote.dto.LibrarySummaryReportDto
import com.greenwood.school.data.remote.dto.PayrollSummaryReportDto
import com.greenwood.school.data.remote.dto.StudentsSummaryReportDto
import com.greenwood.school.data.remote.dto.TeachersSummaryReportDto
import com.greenwood.school.data.remote.dto.TransportSummaryReportDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

/** Endpoints under `/reports`, plus `/analytics/dashboard`. */
interface ReportApi {

    @GET("analytics/dashboard")
    suspend fun getAnalyticsDashboard(): ApiEnvelope<AnalyticsDashboardDto>

    @GET("reports/students-summary")
    suspend fun getStudentsSummary(): ApiEnvelope<StudentsSummaryReportDto>

    @GET("reports/teachers-summary")
    suspend fun getTeachersSummary(): ApiEnvelope<TeachersSummaryReportDto>

    @GET("reports/attendance-summary")
    suspend fun getAttendanceSummary(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("classId") classId: Long? = null,
    ): ApiEnvelope<AttendanceSummaryReportDto>

    @GET("reports/fee-collection")
    suspend fun getFeeCollection(
        @Query("academicYearId") academicYearId: Long? = null,
    ): ApiEnvelope<FeeCollectionReportDto>

    @Streaming
    @GET("reports/fee-collection/export/excel")
    suspend fun exportFeeCollectionExcel(
        @Query("academicYearId") academicYearId: Long? = null,
    ): ResponseBody

    @GET("reports/payroll-summary")
    suspend fun getPayrollSummary(@Query("year") year: Int? = null): ApiEnvelope<PayrollSummaryReportDto>

    @GET("reports/library-summary")
    suspend fun getLibrarySummary(): ApiEnvelope<LibrarySummaryReportDto>

    @GET("reports/transport-summary")
    suspend fun getTransportSummary(): ApiEnvelope<TransportSummaryReportDto>
}

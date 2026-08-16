package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.LeaveApplicationDto
import com.greenwood.school.data.remote.dto.LeaveApplicationRequestDto
import com.greenwood.school.data.remote.dto.MonthlyAttendanceRowDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.StudentAttendanceMarkRequestDto
import com.greenwood.school.data.remote.dto.StudentAttendanceReportRowDto
import com.greenwood.school.data.remote.dto.StudentAttendanceRowDto
import com.greenwood.school.data.remote.dto.StudentAttendanceSummaryDto
import com.greenwood.school.data.remote.dto.TeacherAttendanceMarkRequestDto
import com.greenwood.school.data.remote.dto.TeacherAttendanceReportRowDto
import com.greenwood.school.data.remote.dto.TeacherAttendanceRowDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints under `/attendance` and `/leave-applications`.
 *
 * These use Spring's `Pageable`, so paging is `page`/`size`/`sort=field,dir`
 * (unlike `/students` and friends, which take `sortBy`/`sortDirection`).
 */
interface AttendanceApi {

    /* ---- Student attendance ------------------------------------------------ */

    /** Roster for one class/section/date; `status` is null for unmarked students. */
    @GET("attendance/students")
    suspend fun getStudentMarkingGrid(
        @Query("classId") classId: Long,
        @Query("sectionId") sectionId: Long,
        @Query("date") date: String,
    ): ApiEnvelope<List<StudentAttendanceRowDto>>

    @POST("attendance/students/mark")
    suspend fun markStudentAttendance(@Body request: StudentAttendanceMarkRequestDto): ApiEnvelope<Unit>

    @GET("attendance/students/report")
    suspend fun getStudentAttendanceReport(
        @Query("studentId") studentId: Long? = null,
        @Query("classId") classId: Long? = null,
        @Query("sectionId") sectionId: Long? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "attendanceDate,desc",
    ): ApiEnvelope<PageEnvelope<StudentAttendanceReportRowDto>>

    /**
     * The signed-in student's own attendance summary. Declared before the by-id
     * route to match the backend's mapping order; no id crosses the wire.
     */
    @GET("attendance/students/me/summary")
    suspend fun getOwnAttendanceSummary(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
    ): ApiEnvelope<StudentAttendanceSummaryDto>

    @GET("attendance/students/{studentId}/summary")
    suspend fun getStudentAttendanceSummary(
        @Path("studentId") studentId: Long,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
    ): ApiEnvelope<StudentAttendanceSummaryDto>

    @GET("attendance/students/monthly")
    suspend fun getMonthlyAttendance(
        @Query("classId") classId: Long,
        @Query("sectionId") sectionId: Long,
        @Query("year") year: Int,
        @Query("month") month: Int,
    ): ApiEnvelope<List<MonthlyAttendanceRowDto>>

    /* ---- Teacher attendance ------------------------------------------------ */

    @GET("attendance/teachers")
    suspend fun getTeacherMarkingGrid(@Query("date") date: String): ApiEnvelope<List<TeacherAttendanceRowDto>>

    @POST("attendance/teachers/mark")
    suspend fun markTeacherAttendance(@Body request: TeacherAttendanceMarkRequestDto): ApiEnvelope<Unit>

    @GET("attendance/teachers/report")
    suspend fun getTeacherAttendanceReport(
        @Query("teacherId") teacherId: Long? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "attendanceDate,desc",
    ): ApiEnvelope<PageEnvelope<TeacherAttendanceReportRowDto>>

    /* ---- Leave applications ------------------------------------------------- */

    @GET("leave-applications")
    suspend fun getLeaveApplications(
        @Query("applicantType") applicantType: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "appliedAt,desc",
    ): ApiEnvelope<PageEnvelope<LeaveApplicationDto>>

    @GET("leave-applications/my")
    suspend fun getMyLeaveApplications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "appliedAt,desc",
    ): ApiEnvelope<PageEnvelope<LeaveApplicationDto>>

    @GET("leave-applications/{id}")
    suspend fun getLeaveApplication(@Path("id") id: Long): ApiEnvelope<LeaveApplicationDto>

    @POST("leave-applications")
    suspend fun applyForLeave(@Body request: LeaveApplicationRequestDto): ApiEnvelope<LeaveApplicationDto>

    @PATCH("leave-applications/{id}/approve")
    suspend fun approveLeave(@Path("id") id: Long): ApiEnvelope<LeaveApplicationDto>

    @PATCH("leave-applications/{id}/reject")
    suspend fun rejectLeave(@Path("id") id: Long): ApiEnvelope<LeaveApplicationDto>
}

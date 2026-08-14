package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.ExamDto
import com.greenwood.school.data.remote.dto.ExamRequestDto
import com.greenwood.school.data.remote.dto.ExamResultRowDto
import com.greenwood.school.data.remote.dto.ExamScheduleDto
import com.greenwood.school.data.remote.dto.ExamScheduleRequestDto
import com.greenwood.school.data.remote.dto.ExamTypeDto
import com.greenwood.school.data.remote.dto.ExamTypeRequestDto
import com.greenwood.school.data.remote.dto.MarkDto
import com.greenwood.school.data.remote.dto.MarkRosterRowDto
import com.greenwood.school.data.remote.dto.MarksEntryRequestDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.ReportCardDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** `/exam-types`, `/exams`, `/exam-schedules`, `/marks`. */
interface ExamApi {

    @GET("exam-types")
    suspend fun getExamTypes(): ApiEnvelope<List<ExamTypeDto>>

    @POST("exam-types")
    suspend fun createExamType(@Body request: ExamTypeRequestDto): ApiEnvelope<ExamTypeDto>

    @PUT("exam-types/{id}")
    suspend fun updateExamType(@Path("id") id: Long, @Body request: ExamTypeRequestDto): ApiEnvelope<ExamTypeDto>

    @DELETE("exam-types/{id}")
    suspend fun deleteExamType(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("exams")
    suspend fun getExams(
        @Query("classId") classId: Long? = null,
        @Query("academicYearId") academicYearId: Long? = null,
        @Query("examTypeId") examTypeId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "startDate,desc",
    ): ApiEnvelope<PageEnvelope<ExamDto>>

    @GET("exams/{id}")
    suspend fun getExam(@Path("id") id: Long): ApiEnvelope<ExamDto>

    @POST("exams")
    suspend fun createExam(@Body request: ExamRequestDto): ApiEnvelope<ExamDto>

    @PUT("exams/{id}")
    suspend fun updateExam(@Path("id") id: Long, @Body request: ExamRequestDto): ApiEnvelope<ExamDto>

    @DELETE("exams/{id}")
    suspend fun deleteExam(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("exams/{examId}/results")
    suspend fun getExamResults(
        @Path("examId") examId: Long,
        @Query("classId") classId: Long? = null,
        @Query("sectionId") sectionId: Long? = null,
    ): ApiEnvelope<List<ExamResultRowDto>>

    /* ---- Schedules ----------------------------------------------------------- */

    @GET("exams/{examId}/schedules")
    suspend fun getExamSchedules(@Path("examId") examId: Long): ApiEnvelope<List<ExamScheduleDto>>

    @POST("exams/{examId}/schedules")
    suspend fun createExamSchedule(
        @Path("examId") examId: Long,
        @Body request: ExamScheduleRequestDto,
    ): ApiEnvelope<ExamScheduleDto>

    @PUT("exam-schedules/{id}")
    suspend fun updateExamSchedule(
        @Path("id") id: Long,
        @Body request: ExamScheduleRequestDto,
    ): ApiEnvelope<ExamScheduleDto>

    @DELETE("exam-schedules/{id}")
    suspend fun deleteExamSchedule(@Path("id") id: Long): ApiEnvelope<Unit>

    /* ---- Marks ---------------------------------------------------------------- */

    @GET("marks")
    suspend fun getMarks(
        @Query("examScheduleId") examScheduleId: Long? = null,
        @Query("studentId") studentId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiEnvelope<PageEnvelope<MarkDto>>

    /** Every active student in the schedule's class — the marks-entry grid loads this. */
    @GET("marks/roster")
    suspend fun getMarksRoster(@Query("examScheduleId") examScheduleId: Long): ApiEnvelope<List<MarkRosterRowDto>>

    @POST("marks/entry")
    suspend fun saveMarks(@Body request: MarksEntryRequestDto): ApiEnvelope<Unit>

    @GET("marks/report-card/{studentId}")
    suspend fun getReportCard(
        @Path("studentId") studentId: Long,
        @Query("examId") examId: Long,
    ): ApiEnvelope<ReportCardDto>

    @Streaming
    @GET("marks/report-card/{studentId}/pdf")
    suspend fun downloadReportCard(
        @Path("studentId") studentId: Long,
        @Query("examId") examId: Long,
    ): ResponseBody
}

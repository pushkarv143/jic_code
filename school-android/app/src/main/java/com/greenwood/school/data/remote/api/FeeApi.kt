package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.DuesSummaryDto
import com.greenwood.school.data.remote.dto.FeeCategoryDto
import com.greenwood.school.data.remote.dto.FeePaymentDto
import com.greenwood.school.data.remote.dto.FeePaymentRequestDto
import com.greenwood.school.data.remote.dto.FeePaymentResultDto
import com.greenwood.school.data.remote.dto.FeeReceiptDto
import com.greenwood.school.data.remote.dto.FeeStructureDto
import com.greenwood.school.data.remote.dto.FeeStructureRequestDto
import com.greenwood.school.data.remote.dto.GenerateDuesRequestDto
import com.greenwood.school.data.remote.dto.GenerateDuesResultDto
import com.greenwood.school.data.remote.dto.NamedRequestDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.ScholarshipDto
import com.greenwood.school.data.remote.dto.ScholarshipRequestDto
import com.greenwood.school.data.remote.dto.StudentFeeDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** `/fee-categories`, `/fee-structures`, `/student-fees`, `/fee-payments`, `/fees`, `/scholarships`. */
interface FeeApi {

    /* ---- Setup -------------------------------------------------------------- */

    @GET("fee-categories")
    suspend fun getFeeCategories(): ApiEnvelope<List<FeeCategoryDto>>

    @POST("fee-categories")
    suspend fun createFeeCategory(@Body request: NamedRequestDto): ApiEnvelope<FeeCategoryDto>

    @PUT("fee-categories/{id}")
    suspend fun updateFeeCategory(@Path("id") id: Long, @Body request: NamedRequestDto): ApiEnvelope<FeeCategoryDto>

    @DELETE("fee-categories/{id}")
    suspend fun deleteFeeCategory(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("fee-structures")
    suspend fun getFeeStructures(
        @Query("classId") classId: Long? = null,
        @Query("academicYearId") academicYearId: Long? = null,
        @Query("feeCategoryId") feeCategoryId: Long? = null,
    ): ApiEnvelope<List<FeeStructureDto>>

    @POST("fee-structures")
    suspend fun createFeeStructure(@Body request: FeeStructureRequestDto): ApiEnvelope<FeeStructureDto>

    @PUT("fee-structures/{id}")
    suspend fun updateFeeStructure(
        @Path("id") id: Long,
        @Body request: FeeStructureRequestDto,
    ): ApiEnvelope<FeeStructureDto>

    @DELETE("fee-structures/{id}")
    suspend fun deleteFeeStructure(@Path("id") id: Long): ApiEnvelope<Unit>

    /* ---- Per-student dues ---------------------------------------------------- */

    @GET("student-fees")
    suspend fun getStudentFees(
        @Query("studentId") studentId: Long? = null,
        @Query("classId") classId: Long? = null,
        @Query("sectionId") sectionId: Long? = null,
        @Query("status") status: String? = null,
        @Query("academicYearId") academicYearId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "dueDate,asc",
    ): ApiEnvelope<PageEnvelope<StudentFeeDto>>

    @GET("student-fees/{id}")
    suspend fun getStudentFee(@Path("id") id: Long): ApiEnvelope<StudentFeeDto>

    @POST("student-fees/generate")
    suspend fun generateDues(@Body request: GenerateDuesRequestDto): ApiEnvelope<GenerateDuesResultDto>

    /* ---- Payments ------------------------------------------------------------ */

    @POST("fee-payments")
    suspend fun collectPayment(@Body request: FeePaymentRequestDto): ApiEnvelope<FeePaymentResultDto>

    @GET("fee-payments")
    suspend fun getFeePayments(
        @Query("studentId") studentId: Long? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "paymentDate,desc",
    ): ApiEnvelope<PageEnvelope<FeePaymentDto>>

    @GET("fee-payments/{id}/receipt")
    suspend fun getReceipt(@Path("id") id: Long): ApiEnvelope<FeeReceiptDto>

    @Streaming
    @GET("fee-payments/{id}/receipt/pdf")
    suspend fun downloadReceiptPdf(@Path("id") id: Long): ResponseBody

    @GET("fees/dues-summary")
    suspend fun getDuesSummary(
        @Query("classId") classId: Long? = null,
        @Query("academicYearId") academicYearId: Long? = null,
    ): ApiEnvelope<DuesSummaryDto>

    /* ---- Scholarships --------------------------------------------------------- */

    @GET("scholarships")
    suspend fun getScholarships(
        @Query("studentId") studentId: Long? = null,
        @Query("academicYearId") academicYearId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiEnvelope<PageEnvelope<ScholarshipDto>>

    @POST("scholarships")
    suspend fun createScholarship(@Body request: ScholarshipRequestDto): ApiEnvelope<ScholarshipDto>

    @PUT("scholarships/{id}")
    suspend fun updateScholarship(
        @Path("id") id: Long,
        @Body request: ScholarshipRequestDto,
    ): ApiEnvelope<ScholarshipDto>

    @DELETE("scholarships/{id}")
    suspend fun deleteScholarship(@Path("id") id: Long): ApiEnvelope<Unit>
}

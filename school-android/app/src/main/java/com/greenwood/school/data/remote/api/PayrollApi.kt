package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.GeneratePayrollRequestDto
import com.greenwood.school.data.remote.dto.GeneratePayrollResultDto
import com.greenwood.school.data.remote.dto.MarkPaidRequestDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.PayrollDashboardDto
import com.greenwood.school.data.remote.dto.PayrollRunDto
import com.greenwood.school.data.remote.dto.SalarySlipDto
import com.greenwood.school.data.remote.dto.SalaryStructureDto
import com.greenwood.school.data.remote.dto.SalaryStructureRequestDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** `/salary-structures` and `/payroll`. */
interface PayrollApi {

    @GET("salary-structures")
    suspend fun getSalaryStructures(
        @Query("employeeId") employeeId: Long? = null,
        @Query("employeeType") employeeType: String? = null,
    ): ApiEnvelope<List<SalaryStructureDto>>

    @POST("salary-structures")
    suspend fun createSalaryStructure(
        @Body request: SalaryStructureRequestDto,
    ): ApiEnvelope<SalaryStructureDto>

    @PUT("salary-structures/{id}")
    suspend fun updateSalaryStructure(
        @Path("id") id: Long,
        @Body request: SalaryStructureRequestDto,
    ): ApiEnvelope<SalaryStructureDto>

    @DELETE("salary-structures/{id}")
    suspend fun deleteSalaryStructure(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("payroll")
    suspend fun getPayrollRuns(
        @Query("employeeId") employeeId: Long? = null,
        @Query("employeeType") employeeType: String? = null,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "year,desc",
    ): ApiEnvelope<PageEnvelope<PayrollRunDto>>

    @POST("payroll/generate")
    suspend fun generatePayroll(
        @Body request: GeneratePayrollRequestDto,
    ): ApiEnvelope<GeneratePayrollResultDto>

    @PATCH("payroll/{id}/mark-paid")
    suspend fun markPaid(@Path("id") id: Long, @Body request: MarkPaidRequestDto): ApiEnvelope<PayrollRunDto>

    @GET("payroll/{id}/salary-slip")
    suspend fun getSalarySlip(@Path("id") id: Long): ApiEnvelope<SalarySlipDto>

    @Streaming
    @GET("payroll/{id}/salary-slip/pdf")
    suspend fun downloadSalarySlip(@Path("id") id: Long): ResponseBody

    @GET("payroll/dashboard")
    suspend fun getDashboard(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null,
    ): ApiEnvelope<PayrollDashboardDto>
}

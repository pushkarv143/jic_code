package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.HostelDto
import com.greenwood.school.data.remote.dto.HostelFeeDto
import com.greenwood.school.data.remote.dto.HostelFeeRequestDto
import com.greenwood.school.data.remote.dto.HostelRequestDto
import com.greenwood.school.data.remote.dto.HostelRoomDto
import com.greenwood.school.data.remote.dto.HostelRoomRequestDto
import com.greenwood.school.data.remote.dto.HostelStudentDto
import com.greenwood.school.data.remote.dto.HostelStudentRequestDto
import com.greenwood.school.data.remote.dto.HostelVisitorDto
import com.greenwood.school.data.remote.dto.HostelVisitorRequestDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.VacateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** `/hostels`, `/hostel-rooms`, `/hostel-students`, `/hostel-visitors`, `/hostel-fees`. */
interface HostelApi {

    @GET("hostels")
    suspend fun getHostels(): ApiEnvelope<List<HostelDto>>

    @POST("hostels")
    suspend fun createHostel(@Body request: HostelRequestDto): ApiEnvelope<HostelDto>

    @PUT("hostels/{id}")
    suspend fun updateHostel(@Path("id") id: Long, @Body request: HostelRequestDto): ApiEnvelope<HostelDto>

    @DELETE("hostels/{id}")
    suspend fun deleteHostel(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("hostels/{hostelId}/rooms")
    suspend fun getRooms(@Path("hostelId") hostelId: Long): ApiEnvelope<List<HostelRoomDto>>

    @POST("hostels/{hostelId}/rooms")
    suspend fun createRoom(
        @Path("hostelId") hostelId: Long,
        @Body request: HostelRoomRequestDto,
    ): ApiEnvelope<HostelRoomDto>

    @PUT("hostel-rooms/{id}")
    suspend fun updateRoom(@Path("id") id: Long, @Body request: HostelRoomRequestDto): ApiEnvelope<HostelRoomDto>

    @DELETE("hostel-rooms/{id}")
    suspend fun deleteRoom(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("hostel-students")
    suspend fun getResidents(
        @Query("studentId") studentId: Long? = null,
        @Query("roomId") roomId: Long? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "allocationDate,desc",
    ): ApiEnvelope<PageEnvelope<HostelStudentDto>>

    @POST("hostel-students")
    suspend fun allocateRoom(@Body request: HostelStudentRequestDto): ApiEnvelope<HostelStudentDto>

    @PATCH("hostel-students/{id}/vacate")
    suspend fun vacateRoom(@Path("id") id: Long, @Body request: VacateRequestDto): ApiEnvelope<HostelStudentDto>

    @DELETE("hostel-students/{id}")
    suspend fun deleteAllocation(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("hostel-visitors")
    suspend fun getVisitors(
        @Query("studentId") studentId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "checkIn,desc",
    ): ApiEnvelope<PageEnvelope<HostelVisitorDto>>

    @POST("hostel-visitors")
    suspend fun logVisitor(@Body request: HostelVisitorRequestDto): ApiEnvelope<HostelVisitorDto>

    @PATCH("hostel-visitors/{id}/checkout")
    suspend fun checkoutVisitor(@Path("id") id: Long): ApiEnvelope<HostelVisitorDto>

    @GET("hostel-fees")
    suspend fun getHostelFees(
        @Query("studentId") studentId: Long? = null,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null,
        @Query("paidStatus") paidStatus: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "year,desc",
    ): ApiEnvelope<PageEnvelope<HostelFeeDto>>

    @POST("hostel-fees")
    suspend fun createHostelFee(@Body request: HostelFeeRequestDto): ApiEnvelope<HostelFeeDto>

    @PATCH("hostel-fees/{id}/mark-paid")
    suspend fun markHostelFeePaid(@Path("id") id: Long): ApiEnvelope<HostelFeeDto>
}

package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.BusDto
import com.greenwood.school.data.remote.dto.BusRequestDto
import com.greenwood.school.data.remote.dto.DriverDto
import com.greenwood.school.data.remote.dto.DriverRequestDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.PickupPointDto
import com.greenwood.school.data.remote.dto.PickupPointRequestDto
import com.greenwood.school.data.remote.dto.RouteDto
import com.greenwood.school.data.remote.dto.RouteRequestDto
import com.greenwood.school.data.remote.dto.StudentTransportDto
import com.greenwood.school.data.remote.dto.StudentTransportRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** `/drivers`, `/buses`, `/routes`, `/pickup-points`, `/student-transport`. */
interface TransportApi {

    @GET("drivers")
    suspend fun getDrivers(): ApiEnvelope<List<DriverDto>>

    @POST("drivers")
    suspend fun createDriver(@Body request: DriverRequestDto): ApiEnvelope<DriverDto>

    @PUT("drivers/{id}")
    suspend fun updateDriver(@Path("id") id: Long, @Body request: DriverRequestDto): ApiEnvelope<DriverDto>

    @DELETE("drivers/{id}")
    suspend fun deleteDriver(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("buses")
    suspend fun getBuses(): ApiEnvelope<List<BusDto>>

    @POST("buses")
    suspend fun createBus(@Body request: BusRequestDto): ApiEnvelope<BusDto>

    @PUT("buses/{id}")
    suspend fun updateBus(@Path("id") id: Long, @Body request: BusRequestDto): ApiEnvelope<BusDto>

    @DELETE("buses/{id}")
    suspend fun deleteBus(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("routes")
    suspend fun getRoutes(@Query("busId") busId: Long? = null): ApiEnvelope<List<RouteDto>>

    @POST("routes")
    suspend fun createRoute(@Body request: RouteRequestDto): ApiEnvelope<RouteDto>

    @PUT("routes/{id}")
    suspend fun updateRoute(@Path("id") id: Long, @Body request: RouteRequestDto): ApiEnvelope<RouteDto>

    @DELETE("routes/{id}")
    suspend fun deleteRoute(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("routes/{routeId}/pickup-points")
    suspend fun getPickupPoints(@Path("routeId") routeId: Long): ApiEnvelope<List<PickupPointDto>>

    @POST("routes/{routeId}/pickup-points")
    suspend fun createPickupPoint(
        @Path("routeId") routeId: Long,
        @Body request: PickupPointRequestDto,
    ): ApiEnvelope<PickupPointDto>

    @PUT("pickup-points/{id}")
    suspend fun updatePickupPoint(
        @Path("id") id: Long,
        @Body request: PickupPointRequestDto,
    ): ApiEnvelope<PickupPointDto>

    @DELETE("pickup-points/{id}")
    suspend fun deletePickupPoint(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("student-transport")
    suspend fun getStudentTransport(
        @Query("studentId") studentId: Long? = null,
        @Query("routeId") routeId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "id,desc",
    ): ApiEnvelope<PageEnvelope<StudentTransportDto>>

    @POST("student-transport")
    suspend fun assignTransport(@Body request: StudentTransportRequestDto): ApiEnvelope<StudentTransportDto>

    @PUT("student-transport/{id}")
    suspend fun updateTransportAssignment(
        @Path("id") id: Long,
        @Body request: StudentTransportRequestDto,
    ): ApiEnvelope<StudentTransportDto>

    @DELETE("student-transport/{id}")
    suspend fun removeTransportAssignment(@Path("id") id: Long): ApiEnvelope<Unit>
}

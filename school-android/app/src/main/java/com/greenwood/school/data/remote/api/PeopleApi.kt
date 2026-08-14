package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.StaffDto
import com.greenwood.school.data.remote.dto.StatusRequestDto
import com.greenwood.school.data.remote.dto.TeacherAssignmentDto
import com.greenwood.school.data.remote.dto.TeacherDto
import com.greenwood.school.data.remote.dto.TeacherRequestDto
import com.greenwood.school.data.remote.dto.TeacherSelfUpdateRequestDto
import com.greenwood.school.data.remote.dto.UserDto
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

/** `/teachers`, `/staff` and `/users` — the three people directories. */
interface PeopleApi {

    /* ---- Teachers ---------------------------------------------------------- */

    @GET("teachers")
    suspend fun getTeachers(
        @Query("search") search: String? = null,
        @Query("departmentId") departmentId: Long? = null,
        @Query("designationId") designationId: Long? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "id",
        @Query("sortDirection") sortDirection: String = "asc",
    ): ApiEnvelope<PageEnvelope<TeacherDto>>

    /**
     * The signed-in teacher's own record, returned unredacted. Declared before
     * `teachers/{id}` to match the backend's mapping order; no id crosses the wire.
     */
    @GET("teachers/me")
    suspend fun getOwnTeacherProfile(): ApiEnvelope<TeacherDto>

    /** Updates only the contact/qualification fields a teacher may maintain. */
    @PATCH("teachers/me")
    suspend fun updateOwnTeacherProfile(
        @Body request: TeacherSelfUpdateRequestDto,
    ): ApiEnvelope<TeacherDto>

    /** The classes, sections and subjects the signed-in teacher is assigned to. */
    @GET("teachers/me/assignments")
    suspend fun getOwnTeacherAssignments(): ApiEnvelope<List<TeacherAssignmentDto>>

    /**
     * Another teacher's record. Salary, date of birth, address, emergency contact
     * and blood group come back null unless the caller is management, the
     * accountant, or the teacher themselves — see TeacherDto.isRedacted.
     */
    @GET("teachers/{id}")
    suspend fun getTeacher(@Path("id") id: Long): ApiEnvelope<TeacherDto>

    @POST("teachers")
    suspend fun createTeacher(@Body request: TeacherRequestDto): ApiEnvelope<TeacherDto>

    @PUT("teachers/{id}")
    suspend fun updateTeacher(@Path("id") id: Long, @Body request: TeacherRequestDto): ApiEnvelope<TeacherDto>

    @DELETE("teachers/{id}")
    suspend fun deleteTeacher(@Path("id") id: Long)

    @PATCH("teachers/{id}/status")
    suspend fun updateTeacherStatus(
        @Path("id") id: Long,
        @Body request: StatusRequestDto,
    ): ApiEnvelope<TeacherDto>

    @Streaming
    @GET("teachers/{id}/id-card/pdf")
    suspend fun downloadTeacherIdCard(@Path("id") id: Long): ResponseBody

    @Streaming
    @GET("teachers/export/excel")
    suspend fun exportTeachersExcel(): ResponseBody

    /* ---- Staff (read-only directory) --------------------------------------- */

    @GET("staff")
    suspend fun getStaff(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "id",
        @Query("sortDirection") sortDirection: String = "asc",
    ): ApiEnvelope<PageEnvelope<StaffDto>>

    /* ---- User accounts ------------------------------------------------------ */

    @GET("users")
    suspend fun getUsers(
        @Query("search") search: String? = null,
        @Query("role") role: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "id",
        @Query("sortDirection") sortDirection: String = "asc",
    ): ApiEnvelope<PageEnvelope<UserDto>>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Long): ApiEnvelope<UserDto>

    @PATCH("users/{id}/activate")
    suspend fun activateUser(@Path("id") id: Long): ApiEnvelope<UserDto>

    @PATCH("users/{id}/deactivate")
    suspend fun deactivateUser(@Path("id") id: Long): ApiEnvelope<UserDto>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Long)

    /** `/parents/me/children` — the parent portal's entry point. */
    @GET("parents/me/children")
    suspend fun getMyChildren(): ApiEnvelope<List<com.greenwood.school.data.remote.dto.ParentChildDto>>
}

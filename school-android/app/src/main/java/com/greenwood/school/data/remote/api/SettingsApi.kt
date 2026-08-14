package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.AuditLogDto
import com.greenwood.school.data.remote.dto.GlobalSearchResponseDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.PermissionInfoDto
import com.greenwood.school.data.remote.dto.RoleInfoDto
import com.greenwood.school.data.remote.dto.SchoolInfoDto
import com.greenwood.school.data.remote.dto.SystemSettingDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** `/settings`, `/roles`, `/audit-logs`, `/search`. */
interface SettingsApi {

    @GET("settings/school-info")
    suspend fun getSchoolInfo(): ApiEnvelope<SchoolInfoDto>

    @PUT("settings/school-info")
    suspend fun updateSchoolInfo(@Body request: SchoolInfoDto): ApiEnvelope<SchoolInfoDto>

    @GET("settings/system")
    suspend fun getSystemSettings(): ApiEnvelope<List<SystemSettingDto>>

    @PUT("settings/system")
    suspend fun updateSystemSettings(
        @Body request: List<SystemSettingDto>,
    ): ApiEnvelope<List<SystemSettingDto>>

    @GET("roles")
    suspend fun getRoles(): ApiEnvelope<List<RoleInfoDto>>

    @GET("roles/{id}/permissions")
    suspend fun getRolePermissions(@Path("id") id: Long): ApiEnvelope<List<PermissionInfoDto>>

    @GET("audit-logs")
    suspend fun getAuditLogs(
        @Query("userId") userId: Long? = null,
        @Query("entityName") entityName: String? = null,
        @Query("action") action: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "createdAt,desc",
    ): ApiEnvelope<PageEnvelope<AuditLogDto>>

    /** Cross-module quick search, capped at 5 results per category server-side. */
    @GET("search/global")
    suspend fun globalSearch(@Query("query") query: String): ApiEnvelope<GlobalSearchResponseDto>
}

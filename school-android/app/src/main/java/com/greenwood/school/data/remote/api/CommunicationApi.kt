package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.AdmissionEnquiryDto
import com.greenwood.school.data.remote.dto.AdmissionEnquiryRequestDto
import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.BirthdaysResponseDto
import com.greenwood.school.data.remote.dto.CalendarEventDto
import com.greenwood.school.data.remote.dto.CalendarEventRequestDto
import com.greenwood.school.data.remote.dto.NoticeDto
import com.greenwood.school.data.remote.dto.NotificationDto
import com.greenwood.school.data.remote.dto.NotificationSendResultDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.SendNotificationRequestDto
import com.greenwood.school.data.remote.dto.StatusRequestDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** `/notices`, `/events`, `/calendar`, `/notifications`, `/admission-enquiries`. */
interface CommunicationApi {

    /* ---- Notices (multipart: optional attachment) ----------------------------- */

    @GET("notices")
    suspend fun getNotices(
        @Query("targetRole") targetRole: String? = null,
        @Query("includeExpired") includeExpired: Boolean? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "publishedAt,desc",
    ): ApiEnvelope<PageEnvelope<NoticeDto>>

    @GET("notices/{id}")
    suspend fun getNotice(@Path("id") id: Long): ApiEnvelope<NoticeDto>

    @Multipart
    @POST("notices")
    suspend fun createNotice(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("targetRole") targetRole: RequestBody?,
        @Part("expiryDate") expiryDate: RequestBody?,
        @Part file: MultipartBody.Part? = null,
    ): ApiEnvelope<NoticeDto>

    @Multipart
    @PUT("notices/{id}")
    suspend fun updateNotice(
        @Path("id") id: Long,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("targetRole") targetRole: RequestBody?,
        @Part("expiryDate") expiryDate: RequestBody?,
        @Part file: MultipartBody.Part? = null,
    ): ApiEnvelope<NoticeDto>

    @DELETE("notices/{id}")
    suspend fun deleteNotice(@Path("id") id: Long): ApiEnvelope<Unit>

    /* ---- Calendar --------------------------------------------------------------- */

    @GET("events")
    suspend fun getEvents(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("eventType") eventType: String? = null,
    ): ApiEnvelope<List<CalendarEventDto>>

    @POST("events")
    suspend fun createEvent(@Body request: CalendarEventRequestDto): ApiEnvelope<CalendarEventDto>

    @PUT("events/{id}")
    suspend fun updateEvent(
        @Path("id") id: Long,
        @Body request: CalendarEventRequestDto,
    ): ApiEnvelope<CalendarEventDto>

    @DELETE("events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long): ApiEnvelope<Unit>

    /** `month` is 1-12. Returns students and teachers with a birthday that month. */
    @GET("calendar/birthdays")
    suspend fun getBirthdays(@Query("month") month: Int): ApiEnvelope<BirthdaysResponseDto>

    /* ---- Notifications ----------------------------------------------------------- */

    @GET("notifications/my")
    suspend fun getMyNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "sentAt,desc",
    ): ApiEnvelope<PageEnvelope<NotificationDto>>

    @POST("notifications/send")
    suspend fun sendNotification(
        @Body request: SendNotificationRequestDto,
    ): ApiEnvelope<NotificationSendResultDto>

    /* ---- Admission enquiries ------------------------------------------------------ */

    /** Unauthenticated — the only endpoint the app calls before sign-in. */
    @POST("public/admission-enquiries")
    suspend fun submitPublicEnquiry(
        @Body request: AdmissionEnquiryRequestDto,
    ): ApiEnvelope<AdmissionEnquiryDto>

    @GET("admission-enquiries")
    suspend fun getEnquiries(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "appliedAt,desc",
    ): ApiEnvelope<PageEnvelope<AdmissionEnquiryDto>>

    @PATCH("admission-enquiries/{id}/status")
    suspend fun updateEnquiryStatus(
        @Path("id") id: Long,
        @Body request: StatusRequestDto,
    ): ApiEnvelope<AdmissionEnquiryDto>

    @DELETE("admission-enquiries/{id}")
    suspend fun deleteEnquiry(@Path("id") id: Long): ApiEnvelope<Unit>
}

package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.AssignmentDto
import com.greenwood.school.data.remote.dto.AssignmentSubmissionDto
import com.greenwood.school.data.remote.dto.GradeSubmissionRequestDto
import com.greenwood.school.data.remote.dto.OnlineClassDto
import com.greenwood.school.data.remote.dto.OnlineClassRequestDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.StudyMaterialDto
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

/**
 * `/assignments`, `/assignment-submissions`, `/online-classes`.
 *
 * Assignments are multipart because the create/update form carries an optional
 * attachment alongside the scalar fields — the same shape
 * `school-frontend/src/api/assignmentsApi.ts` builds with `FormData`.
 */
interface ClassroomApi {

    @GET("assignments")
    suspend fun getAssignments(
        @Query("classId") classId: Long? = null,
        @Query("sectionId") sectionId: Long? = null,
        @Query("subjectId") subjectId: Long? = null,
        @Query("teacherId") teacherId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "dueDate,desc",
    ): ApiEnvelope<PageEnvelope<AssignmentDto>>

    @GET("assignments/{id}")
    suspend fun getAssignment(@Path("id") id: Long): ApiEnvelope<AssignmentDto>

    /**
     * Study materials visible to the caller. Read-only on mobile: uploading is a
     * desk task involving files a teacher usually has on a computer, so the app
     * covers browsing and opening rather than authoring.
     */
    @GET("study-materials")
    suspend fun getStudyMaterials(
        @Query("classId") classId: Long? = null,
        @Query("sectionId") sectionId: Long? = null,
        @Query("subjectId") subjectId: Long? = null,
        @Query("materialType") materialType: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "createdAt",
        @Query("sortDirection") sortDirection: String = "desc",
    ): ApiEnvelope<PageEnvelope<StudyMaterialDto>>

    /** Re-checks visibility server-side before handing back the file/external URL. */
    @GET("study-materials/{id}/download")
    suspend fun resolveStudyMaterialDownload(@Path("id") id: Long): ApiEnvelope<StudyMaterialDto>

    @Multipart
    @POST("assignments")
    suspend fun createAssignment(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("classId") classId: RequestBody,
        @Part("sectionId") sectionId: RequestBody,
        @Part("subjectId") subjectId: RequestBody,
        @Part("assignedDate") assignedDate: RequestBody,
        @Part("dueDate") dueDate: RequestBody,
        @Part file: MultipartBody.Part? = null,
    ): ApiEnvelope<AssignmentDto>

    @Multipart
    @PUT("assignments/{id}")
    suspend fun updateAssignment(
        @Path("id") id: Long,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("classId") classId: RequestBody,
        @Part("sectionId") sectionId: RequestBody,
        @Part("subjectId") subjectId: RequestBody,
        @Part("assignedDate") assignedDate: RequestBody,
        @Part("dueDate") dueDate: RequestBody,
        @Part file: MultipartBody.Part? = null,
    ): ApiEnvelope<AssignmentDto>

    @DELETE("assignments/{id}")
    suspend fun deleteAssignment(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("assignments/{assignmentId}/submissions")
    suspend fun getSubmissions(
        @Path("assignmentId") assignmentId: Long,
    ): ApiEnvelope<List<AssignmentSubmissionDto>>

    /** A student's own submission for an assignment; 404 when they haven't submitted. */
    @GET("assignments/{assignmentId}/submissions/my")
    suspend fun getMySubmission(
        @Path("assignmentId") assignmentId: Long,
    ): ApiEnvelope<AssignmentSubmissionDto>

    @Multipart
    @POST("assignments/{assignmentId}/submit")
    suspend fun submitAssignment(
        @Path("assignmentId") assignmentId: Long,
        @Part file: MultipartBody.Part,
    ): ApiEnvelope<AssignmentSubmissionDto>

    @PATCH("assignment-submissions/{id}/grade")
    suspend fun gradeSubmission(
        @Path("id") id: Long,
        @Body request: GradeSubmissionRequestDto,
    ): ApiEnvelope<AssignmentSubmissionDto>

    /* ---- Online classes -------------------------------------------------------- */

    @GET("online-classes")
    suspend fun getOnlineClasses(
        @Query("classId") classId: Long? = null,
        @Query("sectionId") sectionId: Long? = null,
        @Query("subjectId") subjectId: Long? = null,
        @Query("teacherId") teacherId: Long? = null,
        @Query("upcoming") upcoming: Boolean? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "scheduledAt,asc",
    ): ApiEnvelope<PageEnvelope<OnlineClassDto>>

    @POST("online-classes")
    suspend fun createOnlineClass(@Body request: OnlineClassRequestDto): ApiEnvelope<OnlineClassDto>

    @PUT("online-classes/{id}")
    suspend fun updateOnlineClass(
        @Path("id") id: Long,
        @Body request: OnlineClassRequestDto,
    ): ApiEnvelope<OnlineClassDto>

    @DELETE("online-classes/{id}")
    suspend fun deleteOnlineClass(@Path("id") id: Long): ApiEnvelope<Unit>
}

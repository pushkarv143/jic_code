package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.GuardianDto
import com.greenwood.school.data.remote.dto.GuardianRequestDto
import com.greenwood.school.data.remote.dto.ImportResultDto
import com.greenwood.school.data.remote.dto.MedicalDetailsDto
import com.greenwood.school.data.remote.dto.MedicalDetailsRequestDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.PromoteStudentsRequestDto
import com.greenwood.school.data.remote.dto.StatusRequestDto
import com.greenwood.school.data.remote.dto.StudentDocumentDto
import com.greenwood.school.data.remote.dto.StudentDto
import com.greenwood.school.data.remote.dto.StudentRequestDto
import com.greenwood.school.data.remote.dto.StudentSelfUpdateRequestDto
import com.greenwood.school.data.remote.dto.TransferStudentRequestDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
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
import retrofit2.http.Streaming

/** Endpoints under `/api/v1/students` — admissions, guardians, medical details, documents. */
interface StudentApi {

    @GET("students")
    suspend fun getStudents(
        @Query("search") search: String? = null,
        @Query("classId") classId: Long? = null,
        @Query("sectionId") sectionId: Long? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "id",
        @Query("sortDirection") sortDirection: String = "asc",
    ): ApiEnvelope<PageEnvelope<StudentDto>>

    /**
     * The signed-in student's own record. Declared before `students/{id}` to match
     * the backend's mapping order and to make it obvious that no id crosses the
     * wire — the server resolves "me" from the token.
     */
    @GET("students/me")
    suspend fun getOwnStudentProfile(): ApiEnvelope<StudentDto>

    /** Updates only the contact fields a student may maintain themselves. */
    @PATCH("students/me")
    suspend fun updateOwnStudentProfile(
        @Body request: StudentSelfUpdateRequestDto,
    ): ApiEnvelope<StudentDto>

    @GET("students/{id}")
    suspend fun getStudent(@Path("id") id: Long): ApiEnvelope<StudentDto>

    @POST("students")
    suspend fun createStudent(@Body request: StudentRequestDto): ApiEnvelope<StudentDto>

    @PUT("students/{id}")
    suspend fun updateStudent(@Path("id") id: Long, @Body request: StudentRequestDto): ApiEnvelope<StudentDto>

    /** Soft delete — the backend returns 204 with no body. */
    @DELETE("students/{id}")
    suspend fun deleteStudent(@Path("id") id: Long)

    @PATCH("students/{id}/status")
    suspend fun updateStudentStatus(
        @Path("id") id: Long,
        @Body request: StatusRequestDto,
    ): ApiEnvelope<StudentDto>

    /**
     * Returns `{ "photoUrl": "..." }` — a bare map, *not* the student.
     * (`school-frontend/src/api/studentsApi.ts` types this as `Student`, which is wrong.)
     */
    @Multipart
    @POST("students/{id}/photo")
    suspend fun uploadStudentPhoto(
        @Path("id") id: Long,
        @Part file: MultipartBody.Part,
    ): ApiEnvelope<Map<String, String>>

    /* ---- Guardians -------------------------------------------------------- */

    @GET("students/{id}/guardians")
    suspend fun getGuardians(@Path("id") id: Long): ApiEnvelope<List<GuardianDto>>

    @POST("students/{id}/guardians")
    suspend fun addGuardian(@Path("id") id: Long, @Body request: GuardianRequestDto): ApiEnvelope<GuardianDto>

    @PUT("students/{id}/guardians/{guardianId}")
    suspend fun updateGuardian(
        @Path("id") id: Long,
        @Path("guardianId") guardianId: Long,
        @Body request: GuardianRequestDto,
    ): ApiEnvelope<GuardianDto>

    @DELETE("students/{id}/guardians/{guardianId}")
    suspend fun deleteGuardian(@Path("id") id: Long, @Path("guardianId") guardianId: Long)

    /* ---- Medical details / documents -------------------------------------- */

    @GET("students/{id}/medical-details")
    suspend fun getMedicalDetails(@Path("id") id: Long): ApiEnvelope<MedicalDetailsDto>

    @PUT("students/{id}/medical-details")
    suspend fun upsertMedicalDetails(
        @Path("id") id: Long,
        @Body request: MedicalDetailsRequestDto,
    ): ApiEnvelope<MedicalDetailsDto>

    @GET("students/{id}/documents")
    suspend fun getDocuments(@Path("id") id: Long): ApiEnvelope<List<StudentDocumentDto>>

    @Multipart
    @POST("students/{id}/documents")
    suspend fun uploadDocument(
        @Path("id") id: Long,
        @Part file: MultipartBody.Part,
        @Query("documentType") documentType: String,
    ): ApiEnvelope<StudentDocumentDto>

    @DELETE("students/{id}/documents/{docId}")
    suspend fun deleteDocument(@Path("id") id: Long, @Path("docId") docId: Long)

    /* ---- Lifecycle transitions -------------------------------------------- */

    @POST("students/promote")
    suspend fun promoteStudents(@Body request: PromoteStudentsRequestDto): ApiEnvelope<Map<String, Int>>

    @POST("students/{id}/transfer")
    suspend fun transferStudent(
        @Path("id") id: Long,
        @Body request: TransferStudentRequestDto,
    ): ApiEnvelope<Unit>

    @POST("students/{id}/mark-alumni")
    suspend fun markAlumni(@Path("id") id: Long): ApiEnvelope<Unit>

    /* ---- Binary endpoints -------------------------------------------------- */

    @Streaming
    @GET("students/{id}/id-card/pdf")
    suspend fun downloadIdCard(@Path("id") id: Long): ResponseBody

    @Streaming
    @GET("students/export/excel")
    suspend fun exportExcel(
        @Query("classId") classId: Long? = null,
        @Query("sectionId") sectionId: Long? = null,
        @Query("status") status: String? = null,
    ): ResponseBody

    @Multipart
    @POST("students/import/excel")
    suspend fun importExcel(@Part file: MultipartBody.Part): ApiEnvelope<ImportResultDto>

    companion object {
        /** Helper so callers don't repeat the multipart part name the backend expects. */
        fun filePart(fileName: String, body: RequestBody): MultipartBody.Part =
            MultipartBody.Part.createFormData("file", fileName, body)
    }
}

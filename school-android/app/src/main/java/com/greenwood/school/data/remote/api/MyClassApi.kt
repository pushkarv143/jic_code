package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.ClassOfficialDto
import com.greenwood.school.data.remote.dto.HomeroomDto
import com.greenwood.school.data.remote.dto.MyClassOfficialRequestDto
import com.greenwood.school.data.remote.dto.MyClassStudentUpdateRequestDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.StudentDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The class teacher's own section, under `/api/v1/my-class`.
 *
 * <b>No class or section id appears anywhere here.</b> Every route resolves the
 * section from `sections.class_teacher_id` for the caller, so there is no parameter
 * to tamper with and the app never needs to know its own section id. That is what
 * separates this from [StudentApi], which is addressed by id and therefore has to
 * check each one against a scope.
 *
 * There is deliberately **no add-student route**. Admitting a pupil creates a login,
 * an admission number and a guardian record and decides which class they join — the
 * office's job, not a class teacher's. Admissions go through `POST /students`, gated
 * on STUDENT_CREATE, which no teaching role holds.
 */
interface MyClassApi {

    @GET("my-class")
    suspend fun myClass(): ApiEnvelope<HomeroomDto>

    @GET("my-class/students")
    suspend fun students(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
        @Query("sortBy") sortBy: String = "rollNumber",
        @Query("sortDirection") sortDirection: String = "asc",
    ): ApiEnvelope<PageEnvelope<StudentDto>>

    /** Edits a student already on the caller's roster. Needs MY_CLASS_ROSTER_MANAGE. */
    @PUT("my-class/students/{studentId}")
    suspend fun updateStudent(
        @Path("studentId") studentId: Long,
        @Body request: MyClassStudentUpdateRequestDto,
    ): ApiEnvelope<StudentDto>

    /** Who holds each post. Needs only MY_CLASS_VIEW — this is roster information. */
    @GET("my-class/officials")
    suspend fun officials(): ApiEnvelope<List<ClassOfficialDto>>

    /**
     * Appoints one of the caller's own students to a post.
     *
     * Gated by MY_CLASS_OFFICIALS_MANAGE, kept separate from the roster grant so a
     * school can split "maintains records" from "names the head boy".
     */
    @POST("my-class/officials")
    suspend fun appointOfficial(@Body request: MyClassOfficialRequestDto): ApiEnvelope<ClassOfficialDto>

    /** Ends an appointment, leaving the post vacant. The record is kept, not deleted. */
    @DELETE("my-class/officials/{officialId}")
    suspend fun endOfficial(@Path("officialId") officialId: Long)
}

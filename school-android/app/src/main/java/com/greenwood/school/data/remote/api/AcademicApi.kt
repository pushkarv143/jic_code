package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.AcademicYearDto
import com.greenwood.school.data.remote.dto.AcademicYearRequestDto
import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.AssignClassTeacherRequestDto
import com.greenwood.school.data.remote.dto.ClassSubjectTeacherDto
import com.greenwood.school.data.remote.dto.ClassSubjectTeacherRequestDto
import com.greenwood.school.data.remote.dto.DepartmentDto
import com.greenwood.school.data.remote.dto.DesignationDto
import com.greenwood.school.data.remote.dto.NamedRequestDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.SchoolClassDto
import com.greenwood.school.data.remote.dto.SchoolClassRequestDto
import com.greenwood.school.data.remote.dto.SectionDto
import com.greenwood.school.data.remote.dto.SectionRequestDto
import com.greenwood.school.data.remote.dto.SubjectDto
import com.greenwood.school.data.remote.dto.SubjectRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Academic setup: `/academic-years`, `/departments`, `/designations`, `/classes`,
 * `/sections`, `/subjects`, `/class-subject-teacher`.
 *
 * Note `/classes` is one of the five endpoints that take explicit
 * `sortBy`/`sortDirection` rather than Spring's `sort=field,dir`.
 */
interface AcademicApi {

    /* ---- Academic years -------------------------------------------------- */

    @GET("academic-years")
    suspend fun getAcademicYears(): ApiEnvelope<List<AcademicYearDto>>

    @POST("academic-years")
    suspend fun createAcademicYear(@Body request: AcademicYearRequestDto): ApiEnvelope<AcademicYearDto>

    @PUT("academic-years/{id}")
    suspend fun updateAcademicYear(
        @Path("id") id: Long,
        @Body request: AcademicYearRequestDto,
    ): ApiEnvelope<AcademicYearDto>

    @DELETE("academic-years/{id}")
    suspend fun deleteAcademicYear(@Path("id") id: Long): ApiEnvelope<Unit>

    @PATCH("academic-years/{id}/set-current")
    suspend fun setCurrentAcademicYear(@Path("id") id: Long): ApiEnvelope<AcademicYearDto>

    /* ---- Departments / designations -------------------------------------- */

    @GET("departments")
    suspend fun getDepartments(): ApiEnvelope<List<DepartmentDto>>

    @POST("departments")
    suspend fun createDepartment(@Body request: NamedRequestDto): ApiEnvelope<DepartmentDto>

    @PUT("departments/{id}")
    suspend fun updateDepartment(@Path("id") id: Long, @Body request: NamedRequestDto): ApiEnvelope<DepartmentDto>

    @DELETE("departments/{id}")
    suspend fun deleteDepartment(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("designations")
    suspend fun getDesignations(): ApiEnvelope<List<DesignationDto>>

    @POST("designations")
    suspend fun createDesignation(@Body request: NamedRequestDto): ApiEnvelope<DesignationDto>

    @PUT("designations/{id}")
    suspend fun updateDesignation(@Path("id") id: Long, @Body request: NamedRequestDto): ApiEnvelope<DesignationDto>

    @DELETE("designations/{id}")
    suspend fun deleteDesignation(@Path("id") id: Long): ApiEnvelope<Unit>

    /* ---- Classes ---------------------------------------------------------- */

    @GET("classes")
    suspend fun getClasses(
        @Query("academicYearId") academicYearId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200,
        @Query("sortBy") sortBy: String = "id",
        @Query("sortDirection") sortDirection: String = "asc",
    ): ApiEnvelope<PageEnvelope<SchoolClassDto>>

    @GET("classes/{id}")
    suspend fun getClass(@Path("id") id: Long): ApiEnvelope<SchoolClassDto>

    @POST("classes")
    suspend fun createClass(@Body request: SchoolClassRequestDto): ApiEnvelope<SchoolClassDto>

    @PUT("classes/{id}")
    suspend fun updateClass(@Path("id") id: Long, @Body request: SchoolClassRequestDto): ApiEnvelope<SchoolClassDto>

    @DELETE("classes/{id}")
    suspend fun deleteClass(@Path("id") id: Long): ApiEnvelope<Unit>

    /* ---- Sections --------------------------------------------------------- */

    @GET("classes/{classId}/sections")
    suspend fun getSections(@Path("classId") classId: Long): ApiEnvelope<List<SectionDto>>

    @POST("classes/{classId}/sections")
    suspend fun createSection(
        @Path("classId") classId: Long,
        @Body request: SectionRequestDto,
    ): ApiEnvelope<SectionDto>

    @PUT("sections/{id}")
    suspend fun updateSection(@Path("id") id: Long, @Body request: SectionRequestDto): ApiEnvelope<SectionDto>

    @DELETE("sections/{id}")
    suspend fun deleteSection(@Path("id") id: Long): ApiEnvelope<Unit>

    @PATCH("sections/{id}/assign-class-teacher")
    suspend fun assignClassTeacher(
        @Path("id") id: Long,
        @Body request: AssignClassTeacherRequestDto,
    ): ApiEnvelope<SectionDto>

    /* ---- Subjects --------------------------------------------------------- */

    @GET("classes/{classId}/subjects")
    suspend fun getSubjects(@Path("classId") classId: Long): ApiEnvelope<List<SubjectDto>>

    @POST("classes/{classId}/subjects")
    suspend fun createSubject(
        @Path("classId") classId: Long,
        @Body request: SubjectRequestDto,
    ): ApiEnvelope<SubjectDto>

    @PUT("subjects/{id}")
    suspend fun updateSubject(@Path("id") id: Long, @Body request: SubjectRequestDto): ApiEnvelope<SubjectDto>

    @DELETE("subjects/{id}")
    suspend fun deleteSubject(@Path("id") id: Long): ApiEnvelope<Unit>

    /* ---- Class / subject / teacher mapping -------------------------------- */

    @GET("class-subject-teacher")
    suspend fun getTeacherMappings(
        @Query("sectionId") sectionId: Long? = null,
        @Query("subjectId") subjectId: Long? = null,
        @Query("teacherId") teacherId: Long? = null,
    ): ApiEnvelope<List<ClassSubjectTeacherDto>>

    @POST("class-subject-teacher")
    suspend fun assignTeacherMapping(
        @Body request: ClassSubjectTeacherRequestDto,
    ): ApiEnvelope<ClassSubjectTeacherDto>

    @DELETE("class-subject-teacher/{id}")
    suspend fun deleteTeacherMapping(@Path("id") id: Long): ApiEnvelope<Unit>
}

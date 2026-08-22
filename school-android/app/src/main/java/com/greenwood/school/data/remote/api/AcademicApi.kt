package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.AcademicYearDto
import com.greenwood.school.data.remote.dto.AcademicYearRequestDto
import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.AssignClassTeacherRequestDto
import com.greenwood.school.data.remote.dto.ClassOfficialDto
import com.greenwood.school.data.remote.dto.ClassOfficialRequestDto
import com.greenwood.school.data.remote.dto.SaveTimetableRequestDto
import com.greenwood.school.data.remote.dto.ClassOverviewDto
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
import com.greenwood.school.data.remote.dto.TimetableSlotDto
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

    /**
     * Strength, people, cross-module stats and setup warnings for one class, in a
     * single response - the figures all derive from the same roster, so fetching
     * them separately risks showing numbers that disagree with each other.
     */
    @GET("classes/{classId}/overview")
    suspend fun getClassOverview(
        @Path("classId") classId: Long,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
    ): ApiEnvelope<ClassOverviewDto>

    /** Current holders of every post in a class (head boy, head girl, monitor, ...). */
    @GET("classes/{classId}/officials")
    suspend fun getClassOfficials(@Path("classId") classId: Long): ApiEnvelope<List<ClassOfficialDto>>

    /** Every holder the class has had, newest appointment first. */
    @GET("classes/{classId}/officials/history")
    suspend fun getClassOfficialHistory(@Path("classId") classId: Long): ApiEnvelope<List<ClassOfficialDto>>

    /**
     * Appoints a student to a post, ending the sitting holder's tenure if there is
     * one. Management-only server-side (CLASS_MANAGE); a class teacher appoints
     * within their own section through MyClassApi instead.
     */
    @POST("classes/{classId}/officials")
    suspend fun appointClassOfficial(
        @Path("classId") classId: Long,
        @Body request: ClassOfficialRequestDto,
    ): ApiEnvelope<ClassOfficialDto>

    /** Ends an appointment, leaving the post vacant. The record is kept, not deleted. */
    @DELETE("classes/{classId}/officials/{officialId}")
    suspend fun endClassOfficial(
        @Path("classId") classId: Long,
        @Path("officialId") officialId: Long,
    )

    /**
     * Every section of a class, for the weekly grid. Slots carry their own clash
     * warnings.
     *
     * <p>Management only, server-side: browsing an arbitrary class's week is the
     * office's job. Teachers and students read [getMyTimetable] instead.
     */
    @GET("timetable/classes/{classId}")
    suspend fun getClassTimetable(@Path("classId") classId: Long): ApiEnvelope<List<TimetableSlotDto>>

    /**
     * The signed-in caller's own week — the periods a teacher teaches, or the week
     * of the class a student is enrolled in. No id in the path: the server resolves
     * whose timetable it is from the token.
     */
    @GET("timetable/me")
    suspend fun getMyTimetable(): ApiEnvelope<List<TimetableSlotDto>>

    /**
     * The caller's own slice of the subject/teacher grid: a teacher's assigned
     * subjects, or the subjects and teachers of a student's own class.
     */
    @GET("class-subject-teacher/me")
    suspend fun getMyTeacherMappings(): ApiEnvelope<List<ClassSubjectTeacherDto>>

    /** One section's week. */
    @GET("timetable/classes/{classId}/sections/{sectionId}")
    suspend fun getSectionTimetable(
        @Path("classId") classId: Long,
        @Path("sectionId") sectionId: Long,
    ): ApiEnvelope<List<TimetableSlotDto>>

    /**
     * Replaces a section's whole week in one call - an empty slot list clears it.
     *
     * Whole-week rather than per-slot because the server deletes and reinserts: a
     * slot-at-a-time save would trip the (section, day, period) unique key mid-swap.
     * Gated on TIMETABLE_MANAGE, which SUPER_ADMIN alone holds by default.
     */
    @PUT("timetable/classes/{classId}/sections/{sectionId}")
    suspend fun saveSectionTimetable(
        @Path("classId") classId: Long,
        @Path("sectionId") sectionId: Long,
        @Body request: SaveTimetableRequestDto,
    ): ApiEnvelope<List<TimetableSlotDto>>

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

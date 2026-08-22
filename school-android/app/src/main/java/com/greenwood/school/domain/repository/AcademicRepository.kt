package com.greenwood.school.domain.repository

import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.data.remote.dto.AcademicYearDto
import com.greenwood.school.data.remote.dto.AcademicYearRequestDto
import com.greenwood.school.data.remote.dto.ClassOfficialDto
import com.greenwood.school.data.remote.dto.ClassOfficialRequestDto
import com.greenwood.school.data.remote.dto.SaveTimetableRequestDto
import com.greenwood.school.data.remote.dto.ClassOverviewDto
import com.greenwood.school.data.remote.dto.ClassSubjectTeacherDto
import com.greenwood.school.data.remote.dto.ClassSubjectTeacherRequestDto
import com.greenwood.school.data.remote.dto.DepartmentDto
import com.greenwood.school.data.remote.dto.DesignationDto
import com.greenwood.school.data.remote.dto.NamedRequestDto
import com.greenwood.school.data.remote.dto.SchoolClassDto
import com.greenwood.school.data.remote.dto.SchoolClassRequestDto
import com.greenwood.school.data.remote.dto.SectionDto
import com.greenwood.school.data.remote.dto.SectionRequestDto
import com.greenwood.school.data.remote.dto.SubjectDto
import com.greenwood.school.data.remote.dto.TimetableSlotDto
import com.greenwood.school.data.remote.dto.SubjectRequestDto

/**
 * Academic reference data. Nearly every other screen depends on this for its
 * class/section/subject/year pickers, which is why the implementation keeps a
 * short-lived in-memory cache of the slow-changing lists.
 */
interface AcademicRepository {

    suspend fun getAcademicYears(forceRefresh: Boolean = false): ApiResult<List<AcademicYearDto>>
    suspend fun getCurrentAcademicYear(): ApiResult<AcademicYearDto?>
    suspend fun createAcademicYear(request: AcademicYearRequestDto): ApiResult<AcademicYearDto>
    suspend fun updateAcademicYear(id: Long, request: AcademicYearRequestDto): ApiResult<AcademicYearDto>
    suspend fun deleteAcademicYear(id: Long): ApiResult<Unit>
    suspend fun setCurrentAcademicYear(id: Long): ApiResult<AcademicYearDto>

    suspend fun getDepartments(forceRefresh: Boolean = false): ApiResult<List<DepartmentDto>>
    suspend fun createDepartment(request: NamedRequestDto): ApiResult<DepartmentDto>
    suspend fun updateDepartment(id: Long, request: NamedRequestDto): ApiResult<DepartmentDto>
    suspend fun deleteDepartment(id: Long): ApiResult<Unit>

    suspend fun getDesignations(forceRefresh: Boolean = false): ApiResult<List<DesignationDto>>
    suspend fun createDesignation(request: NamedRequestDto): ApiResult<DesignationDto>
    suspend fun updateDesignation(id: Long, request: NamedRequestDto): ApiResult<DesignationDto>
    suspend fun deleteDesignation(id: Long): ApiResult<Unit>

    suspend fun getClasses(academicYearId: Long? = null, forceRefresh: Boolean = false): ApiResult<List<SchoolClassDto>>
    suspend fun getClass(id: Long): ApiResult<SchoolClassDto>
    suspend fun createClass(request: SchoolClassRequestDto): ApiResult<SchoolClassDto>
    suspend fun updateClass(id: Long, request: SchoolClassRequestDto): ApiResult<SchoolClassDto>
    suspend fun deleteClass(id: Long): ApiResult<Unit>

    suspend fun getSections(classId: Long): ApiResult<List<SectionDto>>
    suspend fun createSection(classId: Long, request: SectionRequestDto): ApiResult<SectionDto>
    suspend fun updateSection(id: Long, request: SectionRequestDto): ApiResult<SectionDto>
    suspend fun deleteSection(id: Long): ApiResult<Unit>
    suspend fun assignClassTeacher(sectionId: Long, teacherId: Long): ApiResult<SectionDto>

    suspend fun getSubjects(classId: Long): ApiResult<List<SubjectDto>>

    /* ---- Class module: posts and timetable -------------------------------- */

    /** Strength, people, stats and setup warnings for one class, in one call. */
    suspend fun getClassOverview(classId: Long): ApiResult<ClassOverviewDto>

    /** Current post-holders of a class. */
    suspend fun getClassOfficials(classId: Long): ApiResult<List<ClassOfficialDto>>

    /** Every holder the class has had, newest first. */
    suspend fun getClassOfficialHistory(classId: Long): ApiResult<List<ClassOfficialDto>>

    /** The whole class's week, one entry per period. Management only, server-side. */
    suspend fun getClassTimetable(classId: Long): ApiResult<List<TimetableSlotDto>>

    /** Appoints a student to a post school-wide. Management only (CLASS_MANAGE). */
    suspend fun appointClassOfficial(classId: Long, request: ClassOfficialRequestDto): ApiResult<ClassOfficialDto>

    /** Ends an appointment, leaving the post vacant. The record is kept. */
    suspend fun endClassOfficial(classId: Long, officialId: Long): ApiResult<Unit>

    /** One section's week. */
    suspend fun getSectionTimetable(classId: Long, sectionId: Long): ApiResult<List<TimetableSlotDto>>

    /**
     * Replaces a section's whole week. Needs TIMETABLE_MANAGE, which SUPER_ADMIN
     * alone holds by default.
     */
    suspend fun saveSectionTimetable(
        classId: Long,
        sectionId: Long,
        request: SaveTimetableRequestDto,
    ): ApiResult<List<TimetableSlotDto>>

    /**
     * The signed-in user's own week: the periods a teacher teaches, or the week of
     * the class a student is enrolled in. What every non-management role uses, since
     * browsing a class by id is not theirs to do.
     */
    suspend fun getMyTimetable(): ApiResult<List<TimetableSlotDto>>

    /** The caller's own subjects — assigned to teach, or taught to them. */
    suspend fun getMyTeacherMappings(): ApiResult<List<ClassSubjectTeacherDto>>

    suspend fun createSubject(classId: Long, request: SubjectRequestDto): ApiResult<SubjectDto>
    suspend fun updateSubject(id: Long, request: SubjectRequestDto): ApiResult<SubjectDto>
    suspend fun deleteSubject(id: Long): ApiResult<Unit>

    suspend fun getTeacherMappings(
        sectionId: Long? = null,
        subjectId: Long? = null,
        teacherId: Long? = null,
    ): ApiResult<List<ClassSubjectTeacherDto>>

    suspend fun assignTeacherMapping(request: ClassSubjectTeacherRequestDto): ApiResult<ClassSubjectTeacherDto>
    suspend fun deleteTeacherMapping(id: Long): ApiResult<Unit>

    /** Drops the in-memory reference-data cache (used after any academic-setup edit). */
    fun invalidateCache()
}

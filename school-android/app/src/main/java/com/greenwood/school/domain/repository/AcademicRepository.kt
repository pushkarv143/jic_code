package com.greenwood.school.domain.repository

import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.data.remote.dto.AcademicYearDto
import com.greenwood.school.data.remote.dto.AcademicYearRequestDto
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

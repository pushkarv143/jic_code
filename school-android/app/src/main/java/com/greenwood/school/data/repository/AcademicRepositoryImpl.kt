package com.greenwood.school.data.repository

import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.common.IoDispatcher
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.ErrorMapper
import com.greenwood.school.core.network.map
import com.greenwood.school.core.network.onSuccess
import com.greenwood.school.data.remote.api.AcademicApi
import com.greenwood.school.data.remote.dto.AcademicYearDto
import com.greenwood.school.data.remote.dto.AcademicYearRequestDto
import com.greenwood.school.data.remote.dto.AssignClassTeacherRequestDto
import com.greenwood.school.data.remote.dto.ClassOfficialDto
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
import com.greenwood.school.domain.repository.AcademicRepository
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Academic reference data.
 *
 * The year/department/designation/class lists change a handful of times a year but
 * are needed by almost every picker on every screen. Re-fetching them on each
 * screen open is the single biggest source of avoidable requests on mobile, so
 * they are memoised for the process lifetime and explicitly invalidated whenever
 * the academic-setup screens mutate them.
 */
@Singleton
class AcademicRepositoryImpl @Inject constructor(
    private val api: AcademicApi,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), AcademicRepository {

    private val cache = ConcurrentHashMap<String, Any>()

    /* ---- Academic years ------------------------------------------------------- */

    override suspend fun getAcademicYears(forceRefresh: Boolean): ApiResult<List<AcademicYearDto>> =
        cached(KEY_YEARS, forceRefresh) { call { api.getAcademicYears() } }

    override suspend fun getCurrentAcademicYear(): ApiResult<AcademicYearDto?> =
        getAcademicYears().map { years -> years.firstOrNull { it.isCurrent } ?: years.lastOrNull() }

    override suspend fun createAcademicYear(request: AcademicYearRequestDto): ApiResult<AcademicYearDto> =
        call { api.createAcademicYear(request) }.invalidatingOnSuccess(KEY_YEARS)

    override suspend fun updateAcademicYear(id: Long, request: AcademicYearRequestDto): ApiResult<AcademicYearDto> =
        call { api.updateAcademicYear(id, request) }.invalidatingOnSuccess(KEY_YEARS)

    override suspend fun deleteAcademicYear(id: Long): ApiResult<Unit> =
        ack { api.deleteAcademicYear(id) }.invalidatingOnSuccess(KEY_YEARS)

    override suspend fun setCurrentAcademicYear(id: Long): ApiResult<AcademicYearDto> =
        call { api.setCurrentAcademicYear(id) }.invalidatingOnSuccess(KEY_YEARS)

    /* ---- Departments / designations -------------------------------------------- */

    override suspend fun getDepartments(forceRefresh: Boolean): ApiResult<List<DepartmentDto>> =
        cached(KEY_DEPARTMENTS, forceRefresh) { call { api.getDepartments() } }

    override suspend fun createDepartment(request: NamedRequestDto): ApiResult<DepartmentDto> =
        call { api.createDepartment(request) }.invalidatingOnSuccess(KEY_DEPARTMENTS)

    override suspend fun updateDepartment(id: Long, request: NamedRequestDto): ApiResult<DepartmentDto> =
        call { api.updateDepartment(id, request) }.invalidatingOnSuccess(KEY_DEPARTMENTS)

    override suspend fun deleteDepartment(id: Long): ApiResult<Unit> =
        ack { api.deleteDepartment(id) }.invalidatingOnSuccess(KEY_DEPARTMENTS)

    override suspend fun getDesignations(forceRefresh: Boolean): ApiResult<List<DesignationDto>> =
        cached(KEY_DESIGNATIONS, forceRefresh) { call { api.getDesignations() } }

    override suspend fun createDesignation(request: NamedRequestDto): ApiResult<DesignationDto> =
        call { api.createDesignation(request) }.invalidatingOnSuccess(KEY_DESIGNATIONS)

    override suspend fun updateDesignation(id: Long, request: NamedRequestDto): ApiResult<DesignationDto> =
        call { api.updateDesignation(id, request) }.invalidatingOnSuccess(KEY_DESIGNATIONS)

    override suspend fun deleteDesignation(id: Long): ApiResult<Unit> =
        ack { api.deleteDesignation(id) }.invalidatingOnSuccess(KEY_DESIGNATIONS)

    /* ---- Classes ----------------------------------------------------------------- */

    override suspend fun getClasses(academicYearId: Long?, forceRefresh: Boolean): ApiResult<List<SchoolClassDto>> =
        cached("$KEY_CLASSES:$academicYearId", forceRefresh) {
            paged {
                api.getClasses(
                    academicYearId = academicYearId,
                    page = 0,
                    size = Constants.PICKER_PAGE_SIZE,
                    sortBy = "id",
                    sortDirection = "asc",
                )
            }.map { it.items }
        }

    override suspend fun getClass(id: Long): ApiResult<SchoolClassDto> = call { api.getClass(id) }

    override suspend fun createClass(request: SchoolClassRequestDto): ApiResult<SchoolClassDto> =
        call { api.createClass(request) }.invalidatingAllClasses()

    override suspend fun updateClass(id: Long, request: SchoolClassRequestDto): ApiResult<SchoolClassDto> =
        call { api.updateClass(id, request) }.invalidatingAllClasses()

    override suspend fun deleteClass(id: Long): ApiResult<Unit> =
        ack { api.deleteClass(id) }.invalidatingAllClasses()

    /* ---- Sections ------------------------------------------------------------------ */

    override suspend fun getSections(classId: Long): ApiResult<List<SectionDto>> =
        call { api.getSections(classId) }

    override suspend fun createSection(classId: Long, request: SectionRequestDto): ApiResult<SectionDto> =
        call { api.createSection(classId, request) }

    override suspend fun updateSection(id: Long, request: SectionRequestDto): ApiResult<SectionDto> =
        call { api.updateSection(id, request) }

    override suspend fun deleteSection(id: Long): ApiResult<Unit> = ack { api.deleteSection(id) }

    override suspend fun assignClassTeacher(sectionId: Long, teacherId: Long): ApiResult<SectionDto> =
        call { api.assignClassTeacher(sectionId, AssignClassTeacherRequestDto(teacherId)) }

    /* ---- Subjects -------------------------------------------------------------------- */

    override suspend fun getSubjects(classId: Long): ApiResult<List<SubjectDto>> =
        call { api.getSubjects(classId) }

    /* ---- Class module ------------------------------------------------------ */

    override suspend fun getClassOverview(classId: Long): ApiResult<ClassOverviewDto> =
        call { api.getClassOverview(classId) }

    override suspend fun getClassOfficials(classId: Long): ApiResult<List<ClassOfficialDto>> =
        call { api.getClassOfficials(classId) }

    override suspend fun getClassOfficialHistory(classId: Long): ApiResult<List<ClassOfficialDto>> =
        call { api.getClassOfficialHistory(classId) }

    override suspend fun getClassTimetable(classId: Long): ApiResult<List<TimetableSlotDto>> =
        call { api.getClassTimetable(classId) }

    override suspend fun createSubject(classId: Long, request: SubjectRequestDto): ApiResult<SubjectDto> =
        call { api.createSubject(classId, request) }

    override suspend fun updateSubject(id: Long, request: SubjectRequestDto): ApiResult<SubjectDto> =
        call { api.updateSubject(id, request) }

    override suspend fun deleteSubject(id: Long): ApiResult<Unit> = ack { api.deleteSubject(id) }

    /* ---- Teacher mappings -------------------------------------------------------------- */

    override suspend fun getTeacherMappings(
        sectionId: Long?,
        subjectId: Long?,
        teacherId: Long?,
    ): ApiResult<List<ClassSubjectTeacherDto>> =
        call { api.getTeacherMappings(sectionId, subjectId, teacherId) }

    override suspend fun assignTeacherMapping(
        request: ClassSubjectTeacherRequestDto,
    ): ApiResult<ClassSubjectTeacherDto> = call { api.assignTeacherMapping(request) }

    override suspend fun deleteTeacherMapping(id: Long): ApiResult<Unit> =
        ack { api.deleteTeacherMapping(id) }

    override fun invalidateCache() = cache.clear()

    /* ---- Cache helpers ------------------------------------------------------------------ */

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> cached(
        key: String,
        forceRefresh: Boolean,
        loader: suspend () -> ApiResult<T>,
    ): ApiResult<T> {
        if (!forceRefresh) {
            (cache[key] as? T)?.let { return ApiResult.Success(it) }
        }
        return loader().onSuccess { cache[key] = it }
    }

    private fun <T> ApiResult<T>.invalidatingOnSuccess(key: String): ApiResult<T> =
        onSuccess { cache.remove(key) }

    /** Class lists are cached per academic year, so drop every variant at once. */
    private fun <T> ApiResult<T>.invalidatingAllClasses(): ApiResult<T> = onSuccess {
        cache.keys.filter { it.startsWith(KEY_CLASSES) }.forEach(cache::remove)
    }

    private companion object {
        const val KEY_YEARS = "academic-years"
        const val KEY_DEPARTMENTS = "departments"
        const val KEY_DESIGNATIONS = "designations"
        const val KEY_CLASSES = "classes"
    }
}

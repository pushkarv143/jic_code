package com.greenwood.school.data.repository

import com.greenwood.school.core.common.DocumentOpener
import com.greenwood.school.core.common.IoDispatcher
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.AppException
import com.greenwood.school.core.network.ErrorMapper
import com.greenwood.school.core.network.map
import com.greenwood.school.core.network.Paged
import com.greenwood.school.data.remote.api.PeopleApi
import com.greenwood.school.data.remote.api.StudentApi
import com.greenwood.school.data.remote.dto.GuardianDto
import com.greenwood.school.data.remote.dto.GuardianRequestDto
import com.greenwood.school.data.remote.dto.ImportResultDto
import com.greenwood.school.data.remote.dto.MedicalDetailsDto
import com.greenwood.school.data.remote.dto.MedicalDetailsRequestDto
import com.greenwood.school.data.remote.dto.ParentChildDto
import com.greenwood.school.data.remote.dto.PromoteStudentsRequestDto
import com.greenwood.school.data.remote.dto.StaffDto
import com.greenwood.school.data.remote.dto.StatusRequestDto
import com.greenwood.school.data.remote.dto.StudentDocumentDto
import com.greenwood.school.data.remote.dto.StudentDto
import com.greenwood.school.data.remote.dto.StudentRequestDto
import com.greenwood.school.data.remote.dto.StudentSelfUpdateRequestDto
import com.greenwood.school.data.remote.dto.TeacherAssignmentDto
import com.greenwood.school.data.remote.dto.TeacherDto
import com.greenwood.school.data.remote.dto.TeacherRequestDto
import com.greenwood.school.data.remote.dto.TeacherSelfUpdateRequestDto
import com.greenwood.school.data.remote.dto.TransferStudentRequestDto
import com.greenwood.school.data.remote.dto.UserDto
import com.greenwood.school.domain.repository.PeopleRepository
import com.greenwood.school.domain.repository.StudentRepository
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepositoryImpl @Inject constructor(
    private val api: StudentApi,
    private val documentOpener: DocumentOpener,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), StudentRepository {

    override suspend fun getStudents(
        search: String?,
        classId: Long?,
        sectionId: Long?,
        status: String?,
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String,
    ): ApiResult<Paged<StudentDto>> = paged {
        api.getStudents(
            search = search?.takeIf { it.isNotBlank() },
            classId = classId,
            sectionId = sectionId,
            status = status,
            page = page,
            size = size,
            sortBy = sortBy,
            sortDirection = sortDirection,
        )
    }

    override suspend fun getStudent(id: Long): ApiResult<StudentDto> = call { api.getStudent(id) }

    override suspend fun getOwnStudentProfile(): ApiResult<StudentDto> =
        call { api.getOwnStudentProfile() }

    override suspend fun updateOwnStudentProfile(
        request: StudentSelfUpdateRequestDto,
    ): ApiResult<StudentDto> = call { api.updateOwnStudentProfile(request) }

    override suspend fun createStudent(request: StudentRequestDto): ApiResult<StudentDto> =
        call { api.createStudent(request) }

    override suspend fun updateStudent(id: Long, request: StudentRequestDto): ApiResult<StudentDto> =
        call { api.updateStudent(id, request) }

    override suspend fun deleteStudent(id: Long): ApiResult<Unit> = ack { api.deleteStudent(id) }

    override suspend fun updateStatus(id: Long, status: String): ApiResult<StudentDto> =
        call { api.updateStudentStatus(id, StatusRequestDto(status)) }

    /** The endpoint answers `{ "photoUrl": "..." }`; callers only need the URL. */
    override suspend fun uploadPhoto(id: Long, file: File): ApiResult<String> =
        call { api.uploadStudentPhoto(id, file.asFilePart()) }
            .map { it["photoUrl"].orEmpty() }

    override suspend fun getGuardians(studentId: Long): ApiResult<List<GuardianDto>> =
        call { api.getGuardians(studentId) }

    override suspend fun addGuardian(studentId: Long, request: GuardianRequestDto): ApiResult<GuardianDto> =
        call { api.addGuardian(studentId, request) }

    override suspend fun updateGuardian(
        studentId: Long,
        guardianId: Long,
        request: GuardianRequestDto,
    ): ApiResult<GuardianDto> = call { api.updateGuardian(studentId, guardianId, request) }

    override suspend fun deleteGuardian(studentId: Long, guardianId: Long): ApiResult<Unit> =
        ack { api.deleteGuardian(studentId, guardianId) }

    override suspend fun getMedicalDetails(studentId: Long): ApiResult<MedicalDetailsDto> =
        call { api.getMedicalDetails(studentId) }

    override suspend fun saveMedicalDetails(
        studentId: Long,
        request: MedicalDetailsRequestDto,
    ): ApiResult<MedicalDetailsDto> = call { api.upsertMedicalDetails(studentId, request) }

    override suspend fun getDocuments(studentId: Long): ApiResult<List<StudentDocumentDto>> =
        call { api.getDocuments(studentId) }

    override suspend fun uploadDocument(
        studentId: Long,
        file: File,
        documentType: String,
    ): ApiResult<StudentDocumentDto> = call { api.uploadDocument(studentId, file.asFilePart(), documentType) }

    override suspend fun deleteDocument(studentId: Long, documentId: Long): ApiResult<Unit> =
        ack { api.deleteDocument(studentId, documentId) }

    override suspend fun promote(
        studentIds: List<Long>,
        toClassId: Long,
        toSectionId: Long,
        academicYearId: Long,
    ): ApiResult<Int> = call {
        api.promoteStudents(PromoteStudentsRequestDto(studentIds, toClassId, toSectionId, academicYearId))
    }.map { it["promoted"] ?: studentIds.size }

    override suspend fun transfer(id: Long, remarks: String?): ApiResult<Unit> =
        ack { api.transferStudent(id, TransferStudentRequestDto(remarks)) }

    override suspend fun markAlumni(id: Long): ApiResult<Unit> = ack { api.markAlumni(id) }

    override suspend fun downloadIdCard(id: Long): ApiResult<File> = execute {
        documentOpener.save(api.downloadIdCard(id), "student-id-card-$id.pdf") ?: failDownload()
    }

    override suspend fun exportExcel(classId: Long?, sectionId: Long?, status: String?): ApiResult<File> = execute {
        documentOpener.save(api.exportExcel(classId, sectionId, status), "students.xlsx") ?: failDownload()
    }

    override suspend fun importExcel(file: File): ApiResult<ImportResultDto> =
        call { api.importExcel(file.asFilePart()) }
}

@Singleton
class PeopleRepositoryImpl @Inject constructor(
    private val api: PeopleApi,
    private val documentOpener: DocumentOpener,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), PeopleRepository {

    override suspend fun getTeachers(
        search: String?,
        departmentId: Long?,
        designationId: Long?,
        status: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<TeacherDto>> = paged {
        api.getTeachers(
            search = search?.takeIf { it.isNotBlank() },
            departmentId = departmentId,
            designationId = designationId,
            status = status,
            page = page,
            size = size,
        )
    }

    override suspend fun getTeacher(id: Long): ApiResult<TeacherDto> = call { api.getTeacher(id) }

    override suspend fun getOwnTeacherProfile(): ApiResult<TeacherDto> =
        call { api.getOwnTeacherProfile() }

    override suspend fun updateOwnTeacherProfile(
        request: TeacherSelfUpdateRequestDto,
    ): ApiResult<TeacherDto> = call { api.updateOwnTeacherProfile(request) }

    override suspend fun getOwnTeacherAssignments(): ApiResult<List<TeacherAssignmentDto>> =
        call { api.getOwnTeacherAssignments() }

    override suspend fun createTeacher(request: TeacherRequestDto): ApiResult<TeacherDto> =
        call { api.createTeacher(request) }

    override suspend fun updateTeacher(id: Long, request: TeacherRequestDto): ApiResult<TeacherDto> =
        call { api.updateTeacher(id, request) }

    override suspend fun deleteTeacher(id: Long): ApiResult<Unit> = ack { api.deleteTeacher(id) }

    override suspend fun updateTeacherStatus(id: Long, status: String): ApiResult<TeacherDto> =
        call { api.updateTeacherStatus(id, StatusRequestDto(status)) }

    override suspend fun downloadTeacherIdCard(id: Long): ApiResult<File> = execute {
        documentOpener.save(api.downloadTeacherIdCard(id), "teacher-id-card-$id.pdf") ?: failDownload()
    }

    override suspend fun exportTeachersExcel(): ApiResult<File> = execute {
        documentOpener.save(api.exportTeachersExcel(), "teachers.xlsx") ?: failDownload()
    }

    override suspend fun getStaff(search: String?, page: Int, size: Int): ApiResult<Paged<StaffDto>> =
        paged { api.getStaff(search = search?.takeIf { it.isNotBlank() }, page = page, size = size) }

    override suspend fun getUsers(
        search: String?,
        role: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<UserDto>> = paged {
        api.getUsers(search = search?.takeIf { it.isNotBlank() }, role = role, page = page, size = size)
    }

    override suspend fun setUserActive(id: Long, active: Boolean): ApiResult<UserDto> =
        call { if (active) api.activateUser(id) else api.deactivateUser(id) }

    override suspend fun deleteUser(id: Long): ApiResult<Unit> = ack { api.deleteUser(id) }

    override suspend fun getMyChildren(): ApiResult<List<ParentChildDto>> = call { api.getMyChildren() }
}

/* ---- Shared helpers for the two repositories above -------------------------- */

internal fun File.asFilePart(partName: String = "file"): MultipartBody.Part {
    val mediaType = when (extension.lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "pdf" -> "application/pdf"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        else -> "application/octet-stream"
    }.toMediaTypeOrNull()
    return MultipartBody.Part.createFormData(partName, name, asRequestBody(mediaType))
}

/** Writing the download to cache failed (no space, permissions) — surface it as an error. */
internal fun failDownload(): Nothing =
    throw AppException(AppError.Unknown("Could not save the downloaded file to this device."))

package com.greenwood.school.domain.repository

import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.Paged
import com.greenwood.school.data.remote.dto.GuardianDto
import com.greenwood.school.data.remote.dto.GuardianRequestDto
import com.greenwood.school.data.remote.dto.ImportResultDto
import com.greenwood.school.data.remote.dto.MedicalDetailsDto
import com.greenwood.school.data.remote.dto.MedicalDetailsRequestDto
import com.greenwood.school.data.remote.dto.ParentChildDto
import com.greenwood.school.data.remote.dto.StaffDto
import com.greenwood.school.data.remote.dto.StudentDocumentDto
import com.greenwood.school.data.remote.dto.StudentDto
import com.greenwood.school.data.remote.dto.StudentRequestDto
import com.greenwood.school.data.remote.dto.StudentSelfUpdateRequestDto
import com.greenwood.school.data.remote.dto.TeacherAssignmentDto
import com.greenwood.school.data.remote.dto.TeacherDto
import com.greenwood.school.data.remote.dto.TeacherRequestDto
import com.greenwood.school.data.remote.dto.TeacherSelfUpdateRequestDto
import com.greenwood.school.data.remote.dto.UserDto
import java.io.File

/**
 * Repository contracts for the three people directories.
 *
 * Grouped in one file because they are one cohesive slice of the domain and the
 * alternative — a dozen four-line files — buys nothing. Implementations live in
 * `data/repository/PeopleRepositories.kt`.
 */

interface StudentRepository {

    suspend fun getStudents(
        search: String? = null,
        classId: Long? = null,
        sectionId: Long? = null,
        status: String? = null,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "id",
        sortDirection: String = "asc",
    ): ApiResult<Paged<StudentDto>>

    suspend fun getStudent(id: Long): ApiResult<StudentDto>

    /** The signed-in student's own record; STUDENT role only. */
    suspend fun getOwnStudentProfile(): ApiResult<StudentDto>

    /** Updates the contact fields a student may maintain themselves. */
    suspend fun updateOwnStudentProfile(request: StudentSelfUpdateRequestDto): ApiResult<StudentDto>

    suspend fun createStudent(request: StudentRequestDto): ApiResult<StudentDto>
    suspend fun updateStudent(id: Long, request: StudentRequestDto): ApiResult<StudentDto>
    suspend fun deleteStudent(id: Long): ApiResult<Unit>
    suspend fun updateStatus(id: Long, status: String): ApiResult<StudentDto>

    /**
     * Regenerates this student's temporary password and resends it.
     *
     * The username is left alone — it is the student's identity, and a lost password
     * says nothing about it. Every session on the account is ended server-side, and
     * the forced first-login reset is re-armed.
     */
    suspend fun resendCredentials(id: Long): ApiResult<Unit>
    suspend fun uploadPhoto(id: Long, file: File): ApiResult<String>

    suspend fun getGuardians(studentId: Long): ApiResult<List<GuardianDto>>
    suspend fun addGuardian(studentId: Long, request: GuardianRequestDto): ApiResult<GuardianDto>
    suspend fun updateGuardian(studentId: Long, guardianId: Long, request: GuardianRequestDto): ApiResult<GuardianDto>
    suspend fun deleteGuardian(studentId: Long, guardianId: Long): ApiResult<Unit>

    suspend fun getMedicalDetails(studentId: Long): ApiResult<MedicalDetailsDto>
    suspend fun saveMedicalDetails(studentId: Long, request: MedicalDetailsRequestDto): ApiResult<MedicalDetailsDto>

    suspend fun getDocuments(studentId: Long): ApiResult<List<StudentDocumentDto>>
    suspend fun uploadDocument(studentId: Long, file: File, documentType: String): ApiResult<StudentDocumentDto>
    suspend fun deleteDocument(studentId: Long, documentId: Long): ApiResult<Unit>

    suspend fun promote(
        studentIds: List<Long>,
        toClassId: Long,
        toSectionId: Long,
        academicYearId: Long,
    ): ApiResult<Int>

    suspend fun transfer(id: Long, remarks: String?): ApiResult<Unit>
    suspend fun markAlumni(id: Long): ApiResult<Unit>

    /** Downloads the ID-card PDF and returns the cached file ready to open/share. */
    suspend fun downloadIdCard(id: Long): ApiResult<File>

    suspend fun exportExcel(classId: Long?, sectionId: Long?, status: String?): ApiResult<File>
    suspend fun importExcel(file: File): ApiResult<ImportResultDto>
}

interface PeopleRepository {

    suspend fun getTeachers(
        search: String? = null,
        departmentId: Long? = null,
        designationId: Long? = null,
        status: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<TeacherDto>>

    suspend fun getTeacher(id: Long): ApiResult<TeacherDto>

    /** The signed-in teacher's own record, unredacted. TEACHER only. */
    suspend fun getOwnTeacherProfile(): ApiResult<TeacherDto>

    /** Updates the contact/qualification fields a teacher may maintain themselves. */
    suspend fun updateOwnTeacherProfile(request: TeacherSelfUpdateRequestDto): ApiResult<TeacherDto>

    /** The class/section/subject rows the signed-in teacher is assigned to teach. */
    suspend fun getOwnTeacherAssignments(): ApiResult<List<TeacherAssignmentDto>>

    suspend fun createTeacher(request: TeacherRequestDto): ApiResult<TeacherDto>
    suspend fun updateTeacher(id: Long, request: TeacherRequestDto): ApiResult<TeacherDto>
    suspend fun deleteTeacher(id: Long): ApiResult<Unit>
    suspend fun updateTeacherStatus(id: Long, status: String): ApiResult<TeacherDto>
    suspend fun downloadTeacherIdCard(id: Long): ApiResult<File>
    suspend fun exportTeachersExcel(): ApiResult<File>

    suspend fun getStaff(search: String? = null, page: Int = 0, size: Int = 20): ApiResult<Paged<StaffDto>>

    suspend fun getUsers(
        search: String? = null,
        role: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<UserDto>>

    suspend fun setUserActive(id: Long, active: Boolean): ApiResult<UserDto>
    suspend fun deleteUser(id: Long): ApiResult<Unit>

    suspend fun getMyChildren(): ApiResult<List<ParentChildDto>>
}

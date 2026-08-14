package com.greenwood.school.data.remote.dto

import com.greenwood.school.core.common.Formatters
import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Students, guardians, medical details, documents, teachers and staff.       */
/* ------------------------------------------------------------------------- */

@Serializable
data class GuardianDto(
    val id: Long = 0,
    val studentId: Long = 0,
    val name: String,
    val relation: String,
    val occupation: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val isPrimary: Boolean = false,
)

@Serializable
data class GuardianRequestDto(
    val name: String,
    val relation: String,
    val occupation: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val isPrimary: Boolean = false,
)

@Serializable
data class MedicalDetailsDto(
    val id: Long = 0,
    val studentId: Long = 0,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val allergies: String? = null,
    val medicalConditions: String? = null,
    val doctorName: String? = null,
    val doctorContact: String? = null,
)

@Serializable
data class MedicalDetailsRequestDto(
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val allergies: String? = null,
    val medicalConditions: String? = null,
    val doctorName: String? = null,
    val doctorContact: String? = null,
)

@Serializable
data class StudentDocumentDto(
    val id: Long,
    val studentId: Long = 0,
    val documentType: String,
    val fileUrl: String,
    val uploadedAt: String? = null,
)

/**
 * `students` joined with an optional `users` row — `userId` is nullable per the
 * schema, so first/last name can both be null. Always render via [displayName].
 */
@Serializable
data class StudentDto(
    val id: Long,
    val userId: Long? = null,
    val admissionNumber: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val primaryGuardianName: String? = null,
    val primaryGuardianPhone: String? = null,
    val classId: Long = 0,
    val className: String? = null,
    val sectionId: Long = 0,
    val sectionName: String? = null,
    val rollNumber: String? = null,
    val admissionDate: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val bloodGroup: String? = null,
    val religion: String? = null,
    val category: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val photoUrl: String? = null,
    val academicYearId: Long = 0,
    val status: String = "ACTIVE",
    /** Only populated on `GET /students/{id}`. */
    val guardians: List<GuardianDto>? = null,
    val medicalDetails: MedicalDetailsDto? = null,
    val documents: List<StudentDocumentDto>? = null,
) {
    val displayName: String get() = Formatters.personName(firstName, lastName, admissionNumber)

    /** "Class 5 - A", or just the class when the section is missing. */
    val classSection: String
        get() = listOfNotNull(className, sectionName).joinToString(" - ").ifBlank { Formatters.PLACEHOLDER }
}

@Serializable
data class StudentRequestDto(
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val phone: String? = null,
    val classId: Long,
    val sectionId: Long,
    val academicYearId: Long,
    val rollNumber: String,
    val admissionDate: String,
    val dateOfBirth: String,
    val gender: String,
    val bloodGroup: String? = null,
    val religion: String? = null,
    val category: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    /** Only sent on create; the edit form manages guardians through their own endpoints. */
    val guardians: List<GuardianRequestDto>? = null,
)

/**
 * Mirrors the backend's `StudentSelfUpdateRequest` — the contact fields a student
 * may maintain on their own record. Deliberately has no class/section/roll-number
 * field: those stay with the school office.
 */
@Serializable
data class StudentSelfUpdateRequestDto(
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val bloodGroup: String? = null,
)

@Serializable
data class StatusRequestDto(val status: String)

@Serializable
data class PromoteStudentsRequestDto(
    val studentIds: List<Long>,
    val toClassId: Long,
    val toSectionId: Long,
    val academicYearId: Long,
)

@Serializable
data class TransferStudentRequestDto(val remarks: String? = null)

@Serializable
data class TeacherDto(
    val id: Long,
    val userId: Long = 0,
    val employeeId: String? = null,
    val departmentId: Long = 0,
    val departmentName: String? = null,
    val designationId: Long = 0,
    val designationName: String? = null,
    val qualification: String? = null,
    val experienceYears: Int? = null,
    val joiningDate: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val bloodGroup: String? = null,
    val emergencyContact: String? = null,
    val salary: Double? = null,
    val employmentType: String? = null,
    val status: String = "ACTIVE",
    val username: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val photoUrl: String? = null,
    /** Class/section/subject rows; returned by `/teachers/{id}` and `/teachers/me`, not by the list. */
    val assignments: List<TeacherAssignmentDto> = emptyList(),
) {
    val displayName: String get() = Formatters.personName(firstName, lastName, employeeId ?: username)

    /**
     * True when the backend redacted this record — salary and the personal fields
     * come back null for a colleague's row unless the caller is management, the
     * accountant, or the teacher themselves. Used to hide empty sections rather
     * than render a column of dashes.
     */
    val isRedacted: Boolean
        get() = salary == null && dateOfBirth == null && address == null && emergencyContact == null
}

/** One class/section/subject a teacher is assigned to teach. */
@Serializable
data class TeacherAssignmentDto(
    val id: Long,
    val classId: Long = 0,
    val className: String? = null,
    val sectionId: Long = 0,
    val sectionName: String? = null,
    val subjectId: Long = 0,
    val subjectName: String? = null,
)

/**
 * Mirrors the backend's `TeacherSelfUpdateRequest` — the contact and qualification
 * fields a teacher may maintain. No department, designation, salary, employment
 * type or status: those are HR decisions.
 */
@Serializable
data class TeacherSelfUpdateRequestDto(
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val bloodGroup: String? = null,
    val emergencyContact: String? = null,
    val qualification: String? = null,
)

@Serializable
data class TeacherRequestDto(
    val username: String,
    val email: String,
    /** Omitted on edit — the backend keeps the existing hash when this is absent. */
    val password: String? = null,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val gender: String,
    val departmentId: Long,
    val designationId: Long,
    val qualification: String? = null,
    val experienceYears: Int? = null,
    val joiningDate: String,
    val dateOfBirth: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val bloodGroup: String? = null,
    val emergencyContact: String? = null,
    val salary: Double? = null,
    val employmentType: String,
)

/** Read-only staff directory row — `GET /staff`. Powers the payroll employee picker. */
@Serializable
data class StaffDto(
    val id: Long,
    val userId: Long? = null,
    val employeeId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val departmentName: String? = null,
    val designationName: String? = null,
    val status: String = "ACTIVE",
) {
    val displayName: String get() = Formatters.personName(firstName, lastName, employeeId)
}

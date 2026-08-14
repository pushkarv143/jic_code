package com.greenwood.school.data.remote.dto

import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Academic setup: years, departments, designations, classes, sections,       */
/* subjects and the class/section/subject/teacher mapping.                    */
/* Field names mirror SCHEMA_CONTRACT.md verbatim (camelCased).               */
/* ------------------------------------------------------------------------- */

@Serializable
data class AcademicYearDto(
    val id: Long,
    val yearName: String,
    val startDate: String,
    val endDate: String,
    val isCurrent: Boolean = false,
)

@Serializable
data class AcademicYearRequestDto(
    val yearName: String,
    val startDate: String,
    val endDate: String,
)

@Serializable
data class DepartmentDto(
    val id: Long,
    val name: String,
    val description: String? = null,
)

@Serializable
data class DesignationDto(
    val id: Long,
    val name: String,
    val description: String? = null,
)

@Serializable
data class NamedRequestDto(
    val name: String,
    val description: String? = null,
)

@Serializable
data class SchoolClassDto(
    val id: Long,
    val className: String,
    val academicYearId: Long,
    val academicYearName: String? = null,
    val sectionCount: Int? = null,
    val studentCount: Int? = null,
)

@Serializable
data class SchoolClassRequestDto(
    val className: String,
    val academicYearId: Long,
)

@Serializable
data class SectionDto(
    val id: Long,
    val sectionName: String,
    val classId: Long,
    val className: String? = null,
    val classTeacherId: Long? = null,
    val classTeacherName: String? = null,
    val roomNumber: String? = null,
    val capacity: Int? = null,
    val studentCount: Int? = null,
)

@Serializable
data class SectionRequestDto(
    val sectionName: String,
    val roomNumber: String? = null,
    val capacity: Int? = null,
    val classTeacherId: Long? = null,
)

@Serializable
data class AssignClassTeacherRequestDto(val teacherId: Long)

@Serializable
data class SubjectDto(
    val id: Long,
    val subjectName: String,
    val subjectCode: String,
    val classId: Long,
    val isElective: Boolean = false,
)

@Serializable
data class SubjectRequestDto(
    val subjectName: String,
    val subjectCode: String,
    val isElective: Boolean = false,
)

@Serializable
data class ClassSubjectTeacherDto(
    val id: Long,
    val classId: Long,
    val sectionId: Long,
    val sectionName: String? = null,
    val subjectId: Long,
    val subjectName: String? = null,
    val teacherId: Long,
    val teacherName: String? = null,
)

@Serializable
data class ClassSubjectTeacherRequestDto(
    val classId: Long,
    val sectionId: Long,
    val subjectId: Long,
    val teacherId: Long,
)

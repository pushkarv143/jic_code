package com.greenwood.school.data.remote.dto

import com.greenwood.school.core.common.Formatters
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

/* ------------------------------------------------------------------------- */
/* Class module: posts held in a class, and the weekly timetable.             */
/* ------------------------------------------------------------------------- */

/**
 * One student's tenure in one class post.
 *
 * A change of holder closes the sitting tenure and opens a new one rather than
 * overwriting it, so a class keeps its history; [current] is true exactly while
 * [toDate] is null.
 */
@Serializable
data class ClassOfficialDto(
    val id: Long,
    val classId: Long,
    val className: String? = null,
    val sectionId: Long? = null,
    val sectionName: String? = null,
    val studentId: Long? = null,
    val studentName: String? = null,
    val rollNumber: Int? = null,
    val admissionNumber: String? = null,
    val role: String = "",
    val fromDate: String? = null,
    val toDate: String? = null,
    val current: Boolean = false,
    val remarks: String? = null,
) {
    /** HEAD_BOY -> "Head Boy". */
    val roleLabel: String get() = Formatters.humanizeEnum(role)

    val displayName: String get() = studentName?.takeIf { it.isNotBlank() }
        ?: rollNumber?.let { "Roll $it" }
        ?: Formatters.PLACEHOLDER
}

/**
 * Cross-module figures for a class. Every field is nullable on purpose: a class
 * with no marked attendance or no graded exam has nothing to report, and a null
 * says so where a 0 would read as a bad result.
 */
@Serializable
data class ClassStatsDto(
    val attendancePercentage: Double? = null,
    val attendanceMarkedDays: Long? = null,
    val feeDefaulterCount: Long? = null,
    val feeOutstandingAmount: Double? = null,
    val averageMarksPercentage: Double? = null,
    val gradedStudentCount: Long? = null,
)

/** A setup gap worth showing: a subject nobody teaches, a section over capacity, a vacant post. */
@Serializable
data class ClassWarningDto(
    val code: String = "",
    val severity: String = "INFO",
    val message: String = "",
    val sectionId: Long? = null,
    val sectionName: String? = null,
    val subjectId: Long? = null,
    val subjectName: String? = null,
) {
    val isWarning: Boolean get() = severity.equals("WARNING", ignoreCase = true)
}

/**
 * Everything the class overview shows, in one response. Assembled server-side
 * because the strength, the gender split and the warnings all derive from the
 * same roster — fetching them separately risks figures that disagree.
 */
@Serializable
data class ClassOverviewDto(
    val classId: Long,
    val className: String = "",
    val academicYearId: Long? = null,
    val academicYearName: String? = null,
    val totalStudents: Int = 0,
    val totalSections: Int = 0,
    val totalSubjects: Int = 0,
    val totalCapacity: Int? = null,
    val occupancyPercentage: Double? = null,
    val genderSplit: Map<String, Long> = emptyMap(),
    val sections: List<SectionDto> = emptyList(),
    val officials: List<ClassOfficialDto> = emptyList(),
    val subjectTeachers: List<ClassSubjectTeacherDto> = emptyList(),
    val stats: ClassStatsDto? = null,
    val warnings: List<ClassWarningDto> = emptyList(),
)

/**
 * One period of a section's week. [subjectId]/[teacherId] are null for assembly,
 * games and free periods, which [label] names instead. [clashWarning] is set when
 * the slot double-books its teacher or its room — advisory, not blocking, since a
 * timetable being built one section at a time is legitimately in conflict until
 * the rest are filled in.
 */
@Serializable
data class TimetableSlotDto(
    val id: Long? = null,
    val classId: Long? = null,
    val sectionId: Long? = null,
    val sectionName: String? = null,
    val dayOfWeek: String = "",
    val periodNumber: Int = 0,
    val startTime: String? = null,
    val endTime: String? = null,
    val subjectId: Long? = null,
    val subjectName: String? = null,
    val teacherId: Long? = null,
    val teacherName: String? = null,
    val roomNumber: String? = null,
    val label: String? = null,
    val clashWarning: String? = null,
) {
    /** MONDAY -> "Monday". */
    val dayLabel: String get() = Formatters.humanizeEnum(dayOfWeek)

    /** The subject, or the label for a non-teaching period, or a plain "Free". */
    val title: String get() = subjectName?.takeIf { it.isNotBlank() }
        ?: label?.takeIf { it.isNotBlank() }
        ?: "Free"

    val timeRange: String? get() =
        if (startTime != null && endTime != null) {
            "${startTime.take(5)} - ${endTime.take(5)}"
        } else {
            null
        }
}

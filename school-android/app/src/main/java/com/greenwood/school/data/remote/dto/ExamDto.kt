package com.greenwood.school.data.remote.dto

import com.greenwood.school.core.common.Formatters
import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Exam types, exams, schedules, marks, results and report cards.             */
/* ------------------------------------------------------------------------- */

@Serializable
data class ExamTypeDto(val id: Long, val name: String)

@Serializable
data class ExamTypeRequestDto(val name: String)

@Serializable
data class ExamDto(
    val id: Long,
    val examTypeId: Long = 0,
    val examTypeName: String? = null,
    val classId: Long = 0,
    val className: String? = null,
    val academicYearId: Long = 0,
    val academicYearName: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
) {
    /** "Mid Term — Class 5", the label the web results page uses. */
    val title: String
        get() = listOfNotNull(examTypeName, className).joinToString(" — ").ifBlank { "Exam #$id" }
}

@Serializable
data class ExamRequestDto(
    val examTypeId: Long,
    val classId: Long,
    val academicYearId: Long,
    val startDate: String,
    val endDate: String,
)

@Serializable
data class ExamScheduleDto(
    val id: Long,
    val examId: Long = 0,
    val subjectId: Long = 0,
    val subjectName: String? = null,
    val examDate: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val maxMarks: Double = 0.0,
    val roomNumber: String? = null,
)

@Serializable
data class ExamScheduleRequestDto(
    val subjectId: Long,
    val examDate: String,
    val startTime: String,
    val endTime: String,
    val maxMarks: Double,
    val roomNumber: String? = null,
)

@Serializable
data class MarkDto(
    val id: Long,
    val examScheduleId: Long = 0,
    val studentId: Long = 0,
    val studentName: String? = null,
    val rollNumber: String? = null,
    val marksObtained: Double? = null,
    val gradeId: Long? = null,
    val gradeName: String? = null,
    val remarks: String? = null,
    val enteredBy: Long? = null,
)

/**
 * One row per active student in the schedule's class, mark null when not yet
 * entered. This — not `GET /marks` — is what the entry grid loads, so a teacher
 * always sees the full roster.
 */
@Serializable
data class MarkRosterRowDto(
    val studentId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val rollNumber: Long? = null,
    val sectionName: String? = null,
    val marksObtained: Double? = null,
    val maxMarks: Double = 0.0,
    val gradeName: String? = null,
    val remarks: String? = null,
) {
    val displayName: String get() = Formatters.personName(firstName, lastName, rollNumber?.toString())
}

@Serializable
data class MarksEntryRecordDto(
    val studentId: Long,
    val marksObtained: Double,
    val remarks: String? = null,
)

@Serializable
data class MarksEntryRequestDto(
    val examScheduleId: Long,
    val records: List<MarksEntryRecordDto>,
)

@Serializable
data class ReportCardSubjectRowDto(
    val subjectName: String = "",
    val marksObtained: Double = 0.0,
    val maxMarks: Double = 0.0,
    val gradeName: String? = null,
)

@Serializable
data class ReportCardDto(
    val studentName: String = "",
    val className: String = "",
    val sectionName: String = "",
    val examName: String = "",
    val subjects: List<ReportCardSubjectRowDto> = emptyList(),
    val totalObtained: Double = 0.0,
    val totalMax: Double = 0.0,
    val overallPercentage: Double = 0.0,
    val overallGrade: String = "",
)

@Serializable
data class ExamResultRowDto(
    val studentId: Long,
    val studentName: String = "",
    val rollNumber: String? = null,
    val totalObtained: Double = 0.0,
    val totalMax: Double = 0.0,
    val percentage: Double = 0.0,
    val rank: Int = 0,
)

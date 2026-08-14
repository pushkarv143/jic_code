package com.greenwood.school.data.remote.dto

import com.greenwood.school.core.common.Formatters
import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Student + teacher attendance and leave applications.                       */
/* ------------------------------------------------------------------------- */

/**
 * One row of the editable marking grid (`GET /attendance/students`).
 * `status` is null for students not yet marked that day — the grid renders those
 * as "unmarked" rather than defaulting them to PRESENT.
 */
@Serializable
data class StudentAttendanceRowDto(
    val studentId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val rollNumber: String? = null,
    val status: String? = null,
    val remarks: String? = null,
) {
    val displayName: String get() = Formatters.personName(firstName, lastName, rollNumber)
}

@Serializable
data class StudentAttendanceMarkRecordDto(
    val studentId: Long,
    val status: String,
    val remarks: String? = null,
)

@Serializable
data class StudentAttendanceMarkRequestDto(
    val classId: Long,
    val sectionId: Long,
    val attendanceDate: String,
    val records: List<StudentAttendanceMarkRecordDto>,
)

@Serializable
data class StudentAttendanceReportRowDto(
    val id: Long = 0,
    val studentId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val rollNumber: String? = null,
    val classId: Long? = null,
    val className: String? = null,
    val sectionId: Long? = null,
    val sectionName: String? = null,
    val attendanceDate: String,
    val status: String,
    val remarks: String? = null,
) {
    val displayName: String get() = Formatters.personName(firstName, lastName, rollNumber)
}

@Serializable
data class StudentAttendanceSummaryDto(
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val lateDays: Int = 0,
    val halfDays: Int = 0,
    val leaveDays: Int = 0,
    val totalMarkedDays: Int = 0,
    val percentage: Double = 0.0,
)

/** `days` is keyed by day-of-month as a string ("1".."31"). */
@Serializable
data class MonthlyAttendanceRowDto(
    val studentId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val rollNumber: String? = null,
    val days: Map<String, String> = emptyMap(),
) {
    val displayName: String get() = Formatters.personName(firstName, lastName, rollNumber)
}

@Serializable
data class TeacherAttendanceRowDto(
    val teacherId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val employeeId: String? = null,
    val status: String? = null,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val remarks: String? = null,
) {
    val displayName: String get() = Formatters.personName(firstName, lastName, employeeId)
}

@Serializable
data class TeacherAttendanceMarkRecordDto(
    val teacherId: Long,
    val status: String,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val remarks: String? = null,
)

@Serializable
data class TeacherAttendanceMarkRequestDto(
    val attendanceDate: String,
    val records: List<TeacherAttendanceMarkRecordDto>,
)

@Serializable
data class TeacherAttendanceReportRowDto(
    val id: Long = 0,
    val teacherId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val employeeId: String? = null,
    val attendanceDate: String,
    val status: String,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val remarks: String? = null,
) {
    val displayName: String get() = Formatters.personName(firstName, lastName, employeeId)
}

@Serializable
data class LeaveApplicationDto(
    val id: Long,
    val applicantId: Long = 0,
    val applicantType: String = "",
    val applicantName: String? = null,
    val applicantEmail: String? = null,
    val leaveType: String = "",
    val startDate: String,
    val endDate: String,
    val reason: String = "",
    val status: String = "PENDING",
    val approvedBy: Long? = null,
    val approvedByName: String? = null,
    val appliedAt: String? = null,
)

@Serializable
data class LeaveApplicationRequestDto(
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val reason: String,
)

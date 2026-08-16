package com.greenwood.school.domain.repository

import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.Paged
import com.greenwood.school.data.remote.dto.AssignmentDto
import com.greenwood.school.data.remote.dto.StudyMaterialDto
import com.greenwood.school.data.remote.dto.AssignmentSubmissionDto
import com.greenwood.school.data.remote.dto.ExamDto
import com.greenwood.school.data.remote.dto.ExamRequestDto
import com.greenwood.school.data.remote.dto.ExamResultRowDto
import com.greenwood.school.data.remote.dto.ExamScheduleDto
import com.greenwood.school.data.remote.dto.ExamScheduleRequestDto
import com.greenwood.school.data.remote.dto.ExamTypeDto
import com.greenwood.school.data.remote.dto.LeaveApplicationDto
import com.greenwood.school.data.remote.dto.LeaveApplicationRequestDto
import com.greenwood.school.data.remote.dto.MarkRosterRowDto
import com.greenwood.school.data.remote.dto.MarksEntryRecordDto
import com.greenwood.school.data.remote.dto.MonthlyAttendanceRowDto
import com.greenwood.school.data.remote.dto.OnlineClassDto
import com.greenwood.school.data.remote.dto.OnlineClassRequestDto
import com.greenwood.school.data.remote.dto.ReportCardDto
import com.greenwood.school.data.remote.dto.StudentAttendanceMarkRecordDto
import com.greenwood.school.data.remote.dto.StudentAttendanceReportRowDto
import com.greenwood.school.data.remote.dto.StudentAttendanceRowDto
import com.greenwood.school.data.remote.dto.StudentAttendanceSummaryDto
import com.greenwood.school.data.remote.dto.TeacherAttendanceMarkRecordDto
import com.greenwood.school.data.remote.dto.TeacherAttendanceReportRowDto
import com.greenwood.school.data.remote.dto.TeacherAttendanceRowDto
import java.io.File

/** Attendance, leave, exams/marks, assignments and online classes. */

interface AttendanceRepository {

    suspend fun getStudentMarkingGrid(
        classId: Long,
        sectionId: Long,
        date: String,
    ): ApiResult<List<StudentAttendanceRowDto>>

    suspend fun markStudentAttendance(
        classId: Long,
        sectionId: Long,
        date: String,
        records: List<StudentAttendanceMarkRecordDto>,
    ): ApiResult<Unit>

    suspend fun getStudentReport(
        studentId: Long? = null,
        classId: Long? = null,
        sectionId: Long? = null,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<StudentAttendanceReportRowDto>>

    /**
     * The signed-in student's own attendance summary; STUDENT role only.
     *
     * Both bounds are mandatory server-side, so they are non-null here — leaving them
     * optional let a caller omit them and get a 500 that `.getOrNull()` then hid.
     */
    suspend fun getOwnAttendanceSummary(
        startDate: String,
        endDate: String,
    ): ApiResult<StudentAttendanceSummaryDto>

    suspend fun getStudentSummary(
        studentId: Long,
        startDate: String,
        endDate: String,
    ): ApiResult<StudentAttendanceSummaryDto>

    suspend fun getMonthly(
        classId: Long,
        sectionId: Long,
        year: Int,
        month: Int,
    ): ApiResult<List<MonthlyAttendanceRowDto>>

    suspend fun getTeacherMarkingGrid(date: String): ApiResult<List<TeacherAttendanceRowDto>>

    suspend fun markTeacherAttendance(
        date: String,
        records: List<TeacherAttendanceMarkRecordDto>,
    ): ApiResult<Unit>

    suspend fun getTeacherReport(
        teacherId: Long? = null,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<TeacherAttendanceReportRowDto>>

    suspend fun getLeaveApplications(
        applicantType: String? = null,
        status: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<LeaveApplicationDto>>

    suspend fun getMyLeaveApplications(page: Int = 0, size: Int = 20): ApiResult<Paged<LeaveApplicationDto>>
    suspend fun applyForLeave(request: LeaveApplicationRequestDto): ApiResult<LeaveApplicationDto>
    suspend fun approveLeave(id: Long): ApiResult<LeaveApplicationDto>
    suspend fun rejectLeave(id: Long): ApiResult<LeaveApplicationDto>
}

interface ExamRepository {

    suspend fun getExamTypes(): ApiResult<List<ExamTypeDto>>
    suspend fun createExamType(name: String): ApiResult<ExamTypeDto>
    suspend fun updateExamType(id: Long, name: String): ApiResult<ExamTypeDto>
    suspend fun deleteExamType(id: Long): ApiResult<Unit>

    suspend fun getExams(
        classId: Long? = null,
        academicYearId: Long? = null,
        examTypeId: Long? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<ExamDto>>

    suspend fun getExam(id: Long): ApiResult<ExamDto>
    suspend fun createExam(request: ExamRequestDto): ApiResult<ExamDto>
    suspend fun updateExam(id: Long, request: ExamRequestDto): ApiResult<ExamDto>
    suspend fun deleteExam(id: Long): ApiResult<Unit>

    suspend fun getResults(examId: Long, classId: Long? = null, sectionId: Long? = null): ApiResult<List<ExamResultRowDto>>

    suspend fun getSchedules(examId: Long): ApiResult<List<ExamScheduleDto>>
    suspend fun createSchedule(examId: Long, request: ExamScheduleRequestDto): ApiResult<ExamScheduleDto>
    suspend fun updateSchedule(id: Long, request: ExamScheduleRequestDto): ApiResult<ExamScheduleDto>
    suspend fun deleteSchedule(id: Long): ApiResult<Unit>

    suspend fun getMarksRoster(examScheduleId: Long): ApiResult<List<MarkRosterRowDto>>
    suspend fun saveMarks(examScheduleId: Long, records: List<MarksEntryRecordDto>): ApiResult<Unit>
    suspend fun getReportCard(studentId: Long, examId: Long): ApiResult<ReportCardDto>
    suspend fun downloadReportCard(studentId: Long, examId: Long): ApiResult<File>
}

interface ClassroomRepository {

    suspend fun getAssignments(
        classId: Long? = null,
        sectionId: Long? = null,
        subjectId: Long? = null,
        teacherId: Long? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<AssignmentDto>>

    suspend fun getAssignment(id: Long): ApiResult<AssignmentDto>

    /** Study materials visible to the caller; the backend decides which rows those are. */
    suspend fun getStudyMaterials(
        classId: Long? = null,
        sectionId: Long? = null,
        subjectId: Long? = null,
        materialType: String? = null,
        search: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<StudyMaterialDto>>

    /** Re-checks visibility before returning the resource URL to open. */
    suspend fun resolveStudyMaterialDownload(id: Long): ApiResult<StudyMaterialDto>

    suspend fun saveAssignment(
        id: Long?,
        title: String,
        description: String?,
        classId: Long,
        sectionId: Long,
        subjectId: Long,
        assignedDate: String,
        dueDate: String,
        attachment: File?,
    ): ApiResult<AssignmentDto>

    suspend fun deleteAssignment(id: Long): ApiResult<Unit>

    suspend fun getSubmissions(assignmentId: Long): ApiResult<List<AssignmentSubmissionDto>>
    suspend fun getMySubmission(assignmentId: Long): ApiResult<AssignmentSubmissionDto>
    suspend fun submitAssignment(assignmentId: Long, file: File): ApiResult<AssignmentSubmissionDto>
    suspend fun gradeSubmission(id: Long, marks: Double, feedback: String?): ApiResult<AssignmentSubmissionDto>

    suspend fun getOnlineClasses(
        classId: Long? = null,
        sectionId: Long? = null,
        subjectId: Long? = null,
        teacherId: Long? = null,
        upcoming: Boolean? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<OnlineClassDto>>

    suspend fun createOnlineClass(request: OnlineClassRequestDto): ApiResult<OnlineClassDto>
    suspend fun updateOnlineClass(id: Long, request: OnlineClassRequestDto): ApiResult<OnlineClassDto>
    suspend fun deleteOnlineClass(id: Long): ApiResult<Unit>
}

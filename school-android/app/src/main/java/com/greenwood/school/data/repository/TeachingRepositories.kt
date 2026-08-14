package com.greenwood.school.data.repository

import com.greenwood.school.core.common.DocumentOpener
import com.greenwood.school.core.common.IoDispatcher
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.ErrorMapper
import com.greenwood.school.core.network.Paged
import com.greenwood.school.data.remote.api.AttendanceApi
import com.greenwood.school.data.remote.api.ClassroomApi
import com.greenwood.school.data.remote.api.ExamApi
import com.greenwood.school.data.remote.dto.AssignmentDto
import com.greenwood.school.data.remote.dto.StudyMaterialDto
import com.greenwood.school.data.remote.dto.AssignmentSubmissionDto
import com.greenwood.school.data.remote.dto.ExamDto
import com.greenwood.school.data.remote.dto.ExamRequestDto
import com.greenwood.school.data.remote.dto.ExamResultRowDto
import com.greenwood.school.data.remote.dto.ExamScheduleDto
import com.greenwood.school.data.remote.dto.ExamScheduleRequestDto
import com.greenwood.school.data.remote.dto.ExamTypeDto
import com.greenwood.school.data.remote.dto.ExamTypeRequestDto
import com.greenwood.school.data.remote.dto.GradeSubmissionRequestDto
import com.greenwood.school.data.remote.dto.LeaveApplicationDto
import com.greenwood.school.data.remote.dto.LeaveApplicationRequestDto
import com.greenwood.school.data.remote.dto.MarkRosterRowDto
import com.greenwood.school.data.remote.dto.MarksEntryRecordDto
import com.greenwood.school.data.remote.dto.MarksEntryRequestDto
import com.greenwood.school.data.remote.dto.MonthlyAttendanceRowDto
import com.greenwood.school.data.remote.dto.OnlineClassDto
import com.greenwood.school.data.remote.dto.OnlineClassRequestDto
import com.greenwood.school.data.remote.dto.ReportCardDto
import com.greenwood.school.data.remote.dto.StudentAttendanceMarkRecordDto
import com.greenwood.school.data.remote.dto.StudentAttendanceMarkRequestDto
import com.greenwood.school.data.remote.dto.StudentAttendanceReportRowDto
import com.greenwood.school.data.remote.dto.StudentAttendanceRowDto
import com.greenwood.school.data.remote.dto.StudentAttendanceSummaryDto
import com.greenwood.school.data.remote.dto.TeacherAttendanceMarkRecordDto
import com.greenwood.school.data.remote.dto.TeacherAttendanceMarkRequestDto
import com.greenwood.school.data.remote.dto.TeacherAttendanceReportRowDto
import com.greenwood.school.data.remote.dto.TeacherAttendanceRowDto
import com.greenwood.school.domain.repository.AttendanceRepository
import com.greenwood.school.domain.repository.ClassroomRepository
import com.greenwood.school.domain.repository.ExamRepository
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val api: AttendanceApi,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), AttendanceRepository {

    override suspend fun getStudentMarkingGrid(
        classId: Long,
        sectionId: Long,
        date: String,
    ): ApiResult<List<StudentAttendanceRowDto>> = call { api.getStudentMarkingGrid(classId, sectionId, date) }

    override suspend fun markStudentAttendance(
        classId: Long,
        sectionId: Long,
        date: String,
        records: List<StudentAttendanceMarkRecordDto>,
    ): ApiResult<Unit> = ack {
        api.markStudentAttendance(StudentAttendanceMarkRequestDto(classId, sectionId, date, records))
    }

    override suspend fun getStudentReport(
        studentId: Long?,
        classId: Long?,
        sectionId: Long?,
        startDate: String?,
        endDate: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<StudentAttendanceReportRowDto>> = paged {
        api.getStudentAttendanceReport(studentId, classId, sectionId, startDate, endDate, page, size)
    }

    override suspend fun getOwnAttendanceSummary(
        startDate: String?,
        endDate: String?,
    ): ApiResult<StudentAttendanceSummaryDto> =
        call { api.getOwnAttendanceSummary(startDate, endDate) }

    override suspend fun getStudentSummary(
        studentId: Long,
        startDate: String?,
        endDate: String?,
    ): ApiResult<StudentAttendanceSummaryDto> =
        call { api.getStudentAttendanceSummary(studentId, startDate, endDate) }

    override suspend fun getMonthly(
        classId: Long,
        sectionId: Long,
        year: Int,
        month: Int,
    ): ApiResult<List<MonthlyAttendanceRowDto>> = call { api.getMonthlyAttendance(classId, sectionId, year, month) }

    override suspend fun getTeacherMarkingGrid(date: String): ApiResult<List<TeacherAttendanceRowDto>> =
        call { api.getTeacherMarkingGrid(date) }

    override suspend fun markTeacherAttendance(
        date: String,
        records: List<TeacherAttendanceMarkRecordDto>,
    ): ApiResult<Unit> = ack { api.markTeacherAttendance(TeacherAttendanceMarkRequestDto(date, records)) }

    override suspend fun getTeacherReport(
        teacherId: Long?,
        startDate: String?,
        endDate: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<TeacherAttendanceReportRowDto>> = paged {
        api.getTeacherAttendanceReport(teacherId, startDate, endDate, page, size)
    }

    override suspend fun getLeaveApplications(
        applicantType: String?,
        status: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<LeaveApplicationDto>> = paged {
        api.getLeaveApplications(applicantType, status, page, size)
    }

    override suspend fun getMyLeaveApplications(page: Int, size: Int): ApiResult<Paged<LeaveApplicationDto>> =
        paged { api.getMyLeaveApplications(page, size) }

    override suspend fun applyForLeave(request: LeaveApplicationRequestDto): ApiResult<LeaveApplicationDto> =
        call { api.applyForLeave(request) }

    override suspend fun approveLeave(id: Long): ApiResult<LeaveApplicationDto> = call { api.approveLeave(id) }

    override suspend fun rejectLeave(id: Long): ApiResult<LeaveApplicationDto> = call { api.rejectLeave(id) }
}

@Singleton
class ExamRepositoryImpl @Inject constructor(
    private val api: ExamApi,
    private val documentOpener: DocumentOpener,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), ExamRepository {

    override suspend fun getExamTypes(): ApiResult<List<ExamTypeDto>> = call { api.getExamTypes() }

    override suspend fun createExamType(name: String): ApiResult<ExamTypeDto> =
        call { api.createExamType(ExamTypeRequestDto(name)) }

    override suspend fun updateExamType(id: Long, name: String): ApiResult<ExamTypeDto> =
        call { api.updateExamType(id, ExamTypeRequestDto(name)) }

    override suspend fun deleteExamType(id: Long): ApiResult<Unit> = ack { api.deleteExamType(id) }

    override suspend fun getExams(
        classId: Long?,
        academicYearId: Long?,
        examTypeId: Long?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<ExamDto>> = paged { api.getExams(classId, academicYearId, examTypeId, page, size) }

    override suspend fun getExam(id: Long): ApiResult<ExamDto> = call { api.getExam(id) }

    override suspend fun createExam(request: ExamRequestDto): ApiResult<ExamDto> = call { api.createExam(request) }

    override suspend fun updateExam(id: Long, request: ExamRequestDto): ApiResult<ExamDto> =
        call { api.updateExam(id, request) }

    override suspend fun deleteExam(id: Long): ApiResult<Unit> = ack { api.deleteExam(id) }

    override suspend fun getResults(
        examId: Long,
        classId: Long?,
        sectionId: Long?,
    ): ApiResult<List<ExamResultRowDto>> = call { api.getExamResults(examId, classId, sectionId) }

    override suspend fun getSchedules(examId: Long): ApiResult<List<ExamScheduleDto>> =
        call { api.getExamSchedules(examId) }

    override suspend fun createSchedule(examId: Long, request: ExamScheduleRequestDto): ApiResult<ExamScheduleDto> =
        call { api.createExamSchedule(examId, request) }

    override suspend fun updateSchedule(id: Long, request: ExamScheduleRequestDto): ApiResult<ExamScheduleDto> =
        call { api.updateExamSchedule(id, request) }

    override suspend fun deleteSchedule(id: Long): ApiResult<Unit> = ack { api.deleteExamSchedule(id) }

    override suspend fun getMarksRoster(examScheduleId: Long): ApiResult<List<MarkRosterRowDto>> =
        call { api.getMarksRoster(examScheduleId) }

    override suspend fun saveMarks(examScheduleId: Long, records: List<MarksEntryRecordDto>): ApiResult<Unit> =
        ack { api.saveMarks(MarksEntryRequestDto(examScheduleId, records)) }

    override suspend fun getReportCard(studentId: Long, examId: Long): ApiResult<ReportCardDto> =
        call { api.getReportCard(studentId, examId) }

    override suspend fun downloadReportCard(studentId: Long, examId: Long): ApiResult<File> = execute {
        documentOpener.save(
            api.downloadReportCard(studentId, examId),
            "report-card-$studentId-$examId.pdf",
        ) ?: failDownload()
    }
}

@Singleton
class ClassroomRepositoryImpl @Inject constructor(
    private val api: ClassroomApi,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), ClassroomRepository {

    override suspend fun getAssignments(
        classId: Long?,
        sectionId: Long?,
        subjectId: Long?,
        teacherId: Long?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<AssignmentDto>> = paged {
        api.getAssignments(classId, sectionId, subjectId, teacherId, page, size)
    }

    override suspend fun getAssignment(id: Long): ApiResult<AssignmentDto> = call { api.getAssignment(id) }

    override suspend fun getStudyMaterials(
        classId: Long?,
        sectionId: Long?,
        subjectId: Long?,
        materialType: String?,
        search: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<StudyMaterialDto>> = paged {
        api.getStudyMaterials(
            classId = classId,
            sectionId = sectionId,
            subjectId = subjectId,
            materialType = materialType?.takeIf { it.isNotBlank() },
            search = search?.takeIf { it.isNotBlank() },
            page = page,
            size = size,
        )
    }

    override suspend fun resolveStudyMaterialDownload(id: Long): ApiResult<StudyMaterialDto> =
        call { api.resolveStudyMaterialDownload(id) }

    /**
     * Create and update share one entry point because the form is identical and the
     * only difference is whether an id exists — the same shape the web app's
     * `AssignmentFormDialog` uses.
     */
    override suspend fun saveAssignment(
        id: Long?,
        title: String,
        description: String?,
        classId: Long,
        sectionId: Long,
        subjectId: Long,
        assignedDate: String,
        dueDate: String,
        attachment: File?,
    ): ApiResult<AssignmentDto> = call {
        val filePart = attachment?.asFilePart()
        if (id == null) {
            api.createAssignment(
                title = title.asTextPart(),
                description = description?.asTextPart(),
                classId = classId.asTextPart(),
                sectionId = sectionId.asTextPart(),
                subjectId = subjectId.asTextPart(),
                assignedDate = assignedDate.asTextPart(),
                dueDate = dueDate.asTextPart(),
                file = filePart,
            )
        } else {
            api.updateAssignment(
                id = id,
                title = title.asTextPart(),
                description = description?.asTextPart(),
                classId = classId.asTextPart(),
                sectionId = sectionId.asTextPart(),
                subjectId = subjectId.asTextPart(),
                assignedDate = assignedDate.asTextPart(),
                dueDate = dueDate.asTextPart(),
                file = filePart,
            )
        }
    }

    override suspend fun deleteAssignment(id: Long): ApiResult<Unit> = ack { api.deleteAssignment(id) }

    override suspend fun getSubmissions(assignmentId: Long): ApiResult<List<AssignmentSubmissionDto>> =
        call { api.getSubmissions(assignmentId) }

    override suspend fun getMySubmission(assignmentId: Long): ApiResult<AssignmentSubmissionDto> =
        call { api.getMySubmission(assignmentId) }

    override suspend fun submitAssignment(assignmentId: Long, file: File): ApiResult<AssignmentSubmissionDto> =
        call { api.submitAssignment(assignmentId, file.asFilePart()) }

    override suspend fun gradeSubmission(
        id: Long,
        marks: Double,
        feedback: String?,
    ): ApiResult<AssignmentSubmissionDto> = call { api.gradeSubmission(id, GradeSubmissionRequestDto(marks, feedback)) }

    override suspend fun getOnlineClasses(
        classId: Long?,
        sectionId: Long?,
        subjectId: Long?,
        teacherId: Long?,
        upcoming: Boolean?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<OnlineClassDto>> = paged {
        api.getOnlineClasses(classId, sectionId, subjectId, teacherId, upcoming, page, size)
    }

    override suspend fun createOnlineClass(request: OnlineClassRequestDto): ApiResult<OnlineClassDto> =
        call { api.createOnlineClass(request) }

    override suspend fun updateOnlineClass(id: Long, request: OnlineClassRequestDto): ApiResult<OnlineClassDto> =
        call { api.updateOnlineClass(id, request) }

    override suspend fun deleteOnlineClass(id: Long): ApiResult<Unit> = ack { api.deleteOnlineClass(id) }
}

/** Scalar multipart fields must be sent as `text/plain` parts, not JSON. */
private val TEXT_PLAIN = "text/plain".toMediaType()

internal fun String.asTextPart(): RequestBody = toRequestBody(TEXT_PLAIN)

internal fun Long.asTextPart(): RequestBody = toString().toRequestBody(TEXT_PLAIN)

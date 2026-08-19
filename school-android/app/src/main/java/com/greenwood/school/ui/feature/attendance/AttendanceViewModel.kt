package com.greenwood.school.ui.feature.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.isManagement
import com.greenwood.school.core.common.isTeaching
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.dto.MonthlyAttendanceRowDto
import com.greenwood.school.data.remote.dto.SchoolClassDto
import com.greenwood.school.data.remote.dto.SectionDto
import com.greenwood.school.data.remote.dto.StudentAttendanceMarkRecordDto
import com.greenwood.school.data.remote.dto.StudentAttendanceRowDto
import com.greenwood.school.data.remote.dto.StudentAttendanceSummaryDto
import com.greenwood.school.data.remote.dto.TeacherAttendanceMarkRecordDto
import com.greenwood.school.data.remote.dto.TeacherAttendanceRowDto
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.domain.repository.AttendanceRepository
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Backs all four attendance views: the student marking grid, the teacher marking
 * grid, the paginated register and the monthly matrix.
 *
 * One ViewModel rather than four because they share the class/section/date
 * selection — changing the class on the Mark tab should not reset it when the user
 * flips to Report, which is exactly what four separate ViewModels would do.
 */
@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val academicRepository: AcademicRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val user = sessionManager.currentUser
    private val role = Role.from(user?.role)

    /** A student or parent only ever sees their own register, never the marking grid. */
    val canMark: Boolean = role.isManagement || role.isTeaching
    val ownStudentId: Long? = user?.studentId

    private val _state = MutableStateFlow(
        AttendanceUiState(canMark = canMark, isSelfServiceOnly = !canMark),
    )
    val state: StateFlow<AttendanceUiState> = _state.asStateFlow()

    private val reportLoader = PagedLoader(viewModelScope) { page, size ->
        val current = _state.value
        attendanceRepository.getStudentReport(
            studentId = ownStudentId.takeIf { !canMark },
            classId = current.selectedClass?.id,
            sectionId = current.selectedSection?.id,
            startDate = Formatters.apiDate(current.rangeStart),
            endDate = Formatters.apiDate(current.rangeEnd),
            page = page,
            size = size,
        )
    }
    val report = reportLoader.state

    init {
        if (canMark) loadClasses()
        reportLoader.refresh()
        // Only a STUDENT has an "own summary" to load; a parent reads each child's
        // through the My Children screen, and staff have no own register.
        if (role == Role.STUDENT) loadOwnSummary()
    }

    /* ---- Selection ------------------------------------------------------- */

    fun onClassSelected(schoolClass: SchoolClassDto?) {
        _state.update {
            it.copy(selectedClass = schoolClass, selectedSection = null, sections = emptyList(), grid = emptyList())
        }
        schoolClass?.let { loadSections(it.id) }
    }

    fun onSectionSelected(section: SectionDto?) {
        _state.update { it.copy(selectedSection = section) }
        loadGrid()
    }

    fun onDateSelected(date: LocalDate) {
        _state.update { it.copy(date = date) }
        loadGrid()
        loadTeacherGrid()
    }

    fun onRangeSelected(start: LocalDate, end: LocalDate) {
        _state.update { it.copy(rangeStart = start, rangeEnd = end) }
        reportLoader.refresh()
    }

    fun onMonthSelected(year: Int, month: Int) {
        _state.update { it.copy(monthYear = year, month = month) }
        loadMonthly()
    }

    /* ---- Student marking grid --------------------------------------------- */

    fun loadGrid() {
        val current = _state.value
        val classId = current.selectedClass?.id ?: return
        val sectionId = current.selectedSection?.id ?: return

        _state.update { it.copy(isGridLoading = true, gridError = null) }
        viewModelScope.launch {
            val result = attendanceRepository.getStudentMarkingGrid(
                classId = classId,
                sectionId = sectionId,
                date = Formatters.apiDate(current.date),
            )
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        isGridLoading = false,
                        grid = result.data,
                        // Seed the working copy from whatever is already recorded; students
                        // with no row stay unmarked rather than defaulting to PRESENT.
                        marks = result.data.mapNotNull { row -> row.status?.let { s -> row.studentId to s } }.toMap(),
                    )

                    is ApiResult.Failure -> it.copy(isGridLoading = false, gridError = result.error)
                }
            }
        }
    }

    fun setStudentStatus(studentId: Long, status: String) = _state.update {
        it.copy(marks = it.marks + (studentId to status))
    }

    /** Bulk action — the register's most common case is "everyone present but two". */
    fun markAllStudents(status: String) = _state.update { current ->
        current.copy(marks = current.grid.associate { it.studentId to status })
    }

    fun saveStudentAttendance() {
        val current = _state.value
        val classId = current.selectedClass?.id ?: return
        val sectionId = current.selectedSection?.id ?: return
        if (current.marks.isEmpty()) {
            _state.update { it.copy(message = UiMessage("Mark at least one student first.", isError = true)) }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = attendanceRepository.markStudentAttendance(
                classId = classId,
                sectionId = sectionId,
                date = Formatters.apiDate(current.date),
                records = current.marks.map { (studentId, status) ->
                    StudentAttendanceMarkRecordDto(studentId = studentId, status = status)
                },
            )
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        isSaving = false,
                        message = UiMessage.success("Attendance saved for ${current.marks.size} students."),
                    )

                    is ApiResult.Failure -> it.copy(isSaving = false, message = UiMessage.error(result.error))
                }
            }
        }
    }

    /* ---- Teacher marking grid ---------------------------------------------- */

    fun loadTeacherGrid() {
        if (!role.isManagement) return
        _state.update { it.copy(isTeacherGridLoading = true) }
        viewModelScope.launch {
            val result = attendanceRepository.getTeacherMarkingGrid(Formatters.apiDate(_state.value.date))
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        isTeacherGridLoading = false,
                        teacherGrid = result.data,
                        teacherMarks = result.data
                            .mapNotNull { row -> row.status?.let { s -> row.teacherId to s } }
                            .toMap(),
                    )

                    is ApiResult.Failure -> it.copy(
                        isTeacherGridLoading = false,
                        message = UiMessage.error(result.error),
                    )
                }
            }
        }
    }

    fun setTeacherStatus(teacherId: Long, status: String) = _state.update {
        it.copy(teacherMarks = it.teacherMarks + (teacherId to status))
    }

    fun saveTeacherAttendance() {
        val current = _state.value
        if (current.teacherMarks.isEmpty()) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = attendanceRepository.markTeacherAttendance(
                date = Formatters.apiDate(current.date),
                records = current.teacherMarks.map { (teacherId, status) ->
                    TeacherAttendanceMarkRecordDto(teacherId = teacherId, status = status)
                },
            )
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        isSaving = false,
                        message = UiMessage.success("Staff attendance saved."),
                    )

                    is ApiResult.Failure -> it.copy(isSaving = false, message = UiMessage.error(result.error))
                }
            }
        }
    }

    /* ---- Monthly register ---------------------------------------------------- */

    fun loadMonthly() {
        val current = _state.value
        val classId = current.selectedClass?.id ?: return
        val sectionId = current.selectedSection?.id ?: return

        _state.update { it.copy(isMonthlyLoading = true) }
        viewModelScope.launch {
            val result = attendanceRepository.getMonthly(classId, sectionId, current.monthYear, current.month)
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(isMonthlyLoading = false, monthly = result.data)
                    is ApiResult.Failure -> it.copy(
                        isMonthlyLoading = false,
                        message = UiMessage.error(result.error),
                    )
                }
            }
        }
    }

    fun refreshReport() = reportLoader.refresh()
    fun loadMoreReport() = reportLoader.loadMore()
    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun loadClasses() = viewModelScope.launch {
        _state.update { it.copy(classes = academicRepository.getClasses().getOrNull().orEmpty()) }
    }

    /**
     * Loads the class's sections and selects the one there is.
     *
     * The school runs a single section per class, so choosing it was a dropdown with
     * one option — the picker is gone from the screen and the id is resolved here
     * instead. The grid is still fetched by section, which is why the lookup stays.
     */
    private fun loadSections(classId: Long) = viewModelScope.launch {
        val sections = academicRepository.getSections(classId).getOrNull().orEmpty()
        _state.update { it.copy(sections = sections, selectedSection = sections.firstOrNull()) }
        if (sections.isNotEmpty()) loadGrid()
    }

    /**
     * Asks for "mine" rather than for the cached studentId. The by-id endpoint would
     * work too — the guard allows a student their own record — but sending no id
     * leaves nothing to tamper with, and it still resolves if the cached session
     * predates studentId being returned at login.
     */
    private fun loadOwnSummary() = viewModelScope.launch {
        // Both bounds are required by the endpoint — sending neither failed the request and
        // left the card silently empty. The card is headed "This year", so summarise the
        // calendar year to date rather than the Register tab's month range.
        val today = LocalDate.now()
        val summary = attendanceRepository.getOwnAttendanceSummary(
            startDate = Formatters.apiDate(today.withDayOfYear(1)),
            endDate = Formatters.apiDate(today),
        ).getOrNull()
        _state.update { it.copy(ownSummary = summary) }
    }
}

data class AttendanceUiState(
    val canMark: Boolean = false,
    val isSelfServiceOnly: Boolean = false,
    val classes: List<SchoolClassDto> = emptyList(),
    val sections: List<SectionDto> = emptyList(),
    val selectedClass: SchoolClassDto? = null,
    val selectedSection: SectionDto? = null,
    val date: LocalDate = LocalDate.now(),
    val rangeStart: LocalDate = LocalDate.now().withDayOfMonth(1),
    val rangeEnd: LocalDate = LocalDate.now(),
    val monthYear: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,

    val grid: List<StudentAttendanceRowDto> = emptyList(),
    /** Working copy: studentId → status. Saved as one batch. */
    val marks: Map<Long, String> = emptyMap(),
    val isGridLoading: Boolean = false,
    val gridError: AppError? = null,

    val teacherGrid: List<TeacherAttendanceRowDto> = emptyList(),
    val teacherMarks: Map<Long, String> = emptyMap(),
    val isTeacherGridLoading: Boolean = false,

    val monthly: List<MonthlyAttendanceRowDto> = emptyList(),
    val isMonthlyLoading: Boolean = false,

    val ownSummary: StudentAttendanceSummaryDto? = null,

    val isSaving: Boolean = false,
    val message: UiMessage? = null,
) {
    val canSave: Boolean get() = selectedClass != null && selectedSection != null && !isSaving
    val markedCount: Int get() = marks.size
}

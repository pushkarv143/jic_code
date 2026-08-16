package com.greenwood.school.ui.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.isManagement
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.ConnectivityObserver
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.dto.CalendarEventDto
import com.greenwood.school.data.remote.dto.NoticeDto
import com.greenwood.school.domain.repository.AttendanceRepository
import com.greenwood.school.domain.repository.CommunicationRepository
import com.greenwood.school.domain.repository.FeeRepository
import com.greenwood.school.domain.repository.LibraryRepository
import com.greenwood.school.domain.repository.PayrollRepository
import com.greenwood.school.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Role-aware dashboard.
 *
 * The web app ships eight separate dashboard components; on mobile that is one
 * screen whose *card set* varies. What matters more than the layout is which calls
 * go out: each role is only asked for the endpoints its `@PreAuthorize` allows, so
 * a TEACHER never fires `/analytics/dashboard` just to collect a 403.
 *
 * Everything runs concurrently and failures are per-card — one dead endpoint dims
 * one tile instead of blanking the screen.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val payrollRepository: PayrollRepository,
    private val attendanceRepository: AttendanceRepository,
    private val feeRepository: FeeRepository,
    private val libraryRepository: LibraryRepository,
    private val communicationRepository: CommunicationRepository,
    sessionManager: SessionManager,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val user = sessionManager.currentUser
    private val role = Role.from(user?.role)

    private val _state = MutableStateFlow(DashboardUiState(role = role, greetingName = user?.firstName.orEmpty()))
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val today = LocalDate.now()

            coroutineScope {
                // Management + accountant see the school-wide rollups.
                val analytics = if (role.isManagement || role == Role.ACCOUNTANT) {
                    async { reportRepository.getAnalyticsDashboard().getOrNull() }
                } else null

                val payroll = if (role.isManagement || role == Role.ACCOUNTANT) {
                    async { payrollRepository.getDashboard(today.monthValue, today.year).getOrNull() }
                } else null

                val attendance = if (role.isManagement) {
                    async { reportRepository.getAttendanceSummary().getOrNull() }
                } else null

                val fees = if (role.isManagement || role == Role.ACCOUNTANT) {
                    async { reportRepository.getFeeCollection().getOrNull() }
                } else null

                val library = if (role.isManagement || role == Role.LIBRARIAN) {
                    async { libraryRepository.getDashboard().getOrNull() }
                } else null

                // A student (or a parent's first child) gets their own attendance and dues.
                // The date range is required by the endpoint; year-to-date matches the
                // "This year" summary the attendance screen shows for the same student.
                val myAttendance = user?.studentId?.let { studentId ->
                    async {
                        attendanceRepository.getStudentSummary(
                            studentId = studentId,
                            startDate = Formatters.apiDate(today.withDayOfYear(1)),
                            endDate = Formatters.apiDate(today),
                        ).getOrNull()
                    }
                }

                val myFees = user?.studentId?.let { studentId ->
                    async { feeRepository.getStudentFees(studentId = studentId, size = 50).getOrNull() }
                }

                // Notices and upcoming events are visible to every authenticated role.
                val notices = async { communicationRepository.getNotices(size = 5).getOrNull()?.items.orEmpty() }
                val events = async {
                    communicationRepository.getEvents(startDate = Formatters.apiDate(today))
                        .getOrNull()
                        .orEmpty()
                        .filter { event ->
                            Formatters.parseDate(event.eventDate)?.isBefore(today)?.not() ?: true
                        }
                        .sortedBy { it.eventDate }
                        .take(5)
                }

                val outstandingOwn = myFees?.await()?.items?.sumOf { it.balance } ?: 0.0

                _state.update {
                    it.copy(
                        isLoading = false,
                        analytics = analytics?.await(),
                        payroll = payroll?.await(),
                        attendance = attendance?.await(),
                        feeCollection = fees?.await(),
                        library = library?.await(),
                        myAttendance = myAttendance?.await(),
                        myOutstandingFees = outstandingOwn,
                        notices = notices.await(),
                        upcomingEvents = events.await(),
                    )
                }
            }
        }
    }

}

data class DashboardUiState(
    val role: Role = Role.UNKNOWN,
    val greetingName: String = "",
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val analytics: com.greenwood.school.data.remote.dto.AnalyticsDashboardDto? = null,
    val payroll: com.greenwood.school.data.remote.dto.PayrollDashboardDto? = null,
    val attendance: com.greenwood.school.data.remote.dto.AttendanceSummaryReportDto? = null,
    val feeCollection: com.greenwood.school.data.remote.dto.FeeCollectionReportDto? = null,
    val library: com.greenwood.school.data.remote.dto.LibraryDashboardDto? = null,
    val myAttendance: com.greenwood.school.data.remote.dto.StudentAttendanceSummaryDto? = null,
    val myOutstandingFees: Double = 0.0,
    val notices: List<NoticeDto> = emptyList(),
    val upcomingEvents: List<CalendarEventDto> = emptyList(),
)

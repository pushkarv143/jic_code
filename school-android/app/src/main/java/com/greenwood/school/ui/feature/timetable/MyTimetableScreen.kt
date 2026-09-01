package com.greenwood.school.ui.feature.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenwood.school.ui.theme.BrandAmber
import com.greenwood.school.ui.theme.BrandIndigo
import com.greenwood.school.ui.theme.BrandIndigoLight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.isTeaching
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.dto.ClassSubjectTeacherDto
import com.greenwood.school.data.remote.dto.TimetableSlotDto
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.feature.classes.ClassTimetableTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The signed-in user's own timetable.
 *
 * One screen for three roles, because the server decides whose week it is:
 * `timetable/me` answers a teacher with the periods they teach and a student with
 * the week of the class they are enrolled in. Nothing here takes an id, which is
 * the point — browsing another class's week, or a colleague's, is management-only
 * on the server and this screen never asks for it.
 *
 * Read-only, like the rest of the class module on the phone: assigning periods
 * stays on the web app where the grid and clash handling live.
 */
@Composable
fun MyTimetableScreen(
    onBack: (() -> Unit)? = null,
    viewModel: MyTimetableViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "My Timetable",
                subtitle = if (state.isTeacher) {
                    "The periods you teach"
                } else {
                    "Your class week"
                },
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> FullScreenLoader()

                // Only an outright failure blocks the screen. An empty week is not an
                // error — a timetable that has not been published yet is the normal
                // early-term state, and the tabs say so.
                state.error != null && state.slots.isEmpty() && state.mappings.isEmpty() ->
                    ErrorView(error = state.error!!, onRetry = viewModel::refresh)

                else -> {
                    TabRow(selectedTabIndex = tab) {
                        listOf("My week", "My subjects").forEachIndexed { index, label ->
                            Tab(
                                selected = tab == index,
                                onClick = { tab = index },
                                text = { Text(label) },
                            )
                        }
                    }
                    if (tab == 0) {
                        TodayAndWeekTab(slots = state.slots, isTeacher = state.isTeacher)
                    } else {
                        MySubjectsTab(state.mappings, state.slots, state.isTeacher)
                    }
                }
            }
        }
    }
}

/**
 * "My week" tab — shows a "Today's Classes" premium card at the top,
 * then the full week grid below. CRED-level: surfaces what's happening NOW first.
 */
@Composable
private fun TodayAndWeekTab(slots: List<TimetableSlotDto>, isTeacher: Boolean) {
    val todayName = LocalDate.now().dayOfWeek.name // e.g. "MONDAY"
    val todaySlots = slots
        .filter { it.dayOfWeek == todayName }
        .sortedBy { it.periodNumber }

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (todaySlots.isNotEmpty()) {
            item(key = "today-header") {
                TodaySectionHeader()
            }
            items(todaySlots.size, key = { "today-${todaySlots[it].id}" }) { index ->
                TodayPeriodRow(slot = todaySlots[index])
            }
            item(key = "week-divider") {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Text(
                        "  Full week  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
    // Full week tab rendered below the today section via the existing ClassTimetableTab
    ClassTimetableTab(slots)
}

@Composable
private fun TodaySectionHeader() {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMM")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(colors = listOf(BrandIndigo, BrandIndigoLight))
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Today's Classes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                today.format(formatter),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(BrandAmber.copy(alpha = 0.2f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                "LIVE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = BrandAmber,
            )
        }
    }
}

private enum class PeriodStatus { DONE, NOW, UPCOMING }

private fun slotStatus(slot: TimetableSlotDto): PeriodStatus {
    val now = LocalTime.now()
    val fmt = DateTimeFormatter.ofPattern("HH:mm:ss")
    val start = runCatching { LocalTime.parse(slot.startTime ?: "00:00:00", fmt) }.getOrNull() ?: return PeriodStatus.UPCOMING
    val end = runCatching { LocalTime.parse(slot.endTime ?: "23:59:59", fmt) }.getOrNull() ?: return PeriodStatus.UPCOMING
    return when {
        now.isAfter(end) -> PeriodStatus.DONE
        now.isAfter(start) && now.isBefore(end) -> PeriodStatus.NOW
        else -> PeriodStatus.UPCOMING
    }
}

@Composable
private fun TodayPeriodRow(slot: TimetableSlotDto) {
    val status = slotStatus(slot)
    val subjectOrLabel = slot.subjectName ?: slot.label ?: "Period ${slot.periodNumber}"

    val statusColor = when (status) {
        PeriodStatus.NOW -> MaterialTheme.colorScheme.tertiary.let {
            androidx.compose.ui.graphics.Color(0xFF2E7D32) // green
        }
        PeriodStatus.DONE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        PeriodStatus.UPCOMING -> MaterialTheme.colorScheme.primary
    }

    val bgColor = when (status) {
        PeriodStatus.NOW -> statusColor.copy(alpha = 0.08f)
        PeriodStatus.DONE -> Color.Transparent
        PeriodStatus.UPCOMING -> statusColor.copy(alpha = 0.04f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status icon
        Icon(
            imageVector = when (status) {
                PeriodStatus.NOW -> Icons.Outlined.PlayCircle
                PeriodStatus.DONE -> Icons.Outlined.CheckCircle
                PeriodStatus.UPCOMING -> Icons.Outlined.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))

        // Period details
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subjectOrLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (status == PeriodStatus.NOW) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (status == PeriodStatus.DONE)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (status == PeriodStatus.NOW) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("NOW", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = statusColor)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(3.dp))
                Text(
                    "${slot.startTime?.take(5) ?: ""} – ${slot.endTime?.take(5) ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (slot.roomNumber != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.MeetingRoom, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(3.dp))
                    Text(slot.roomNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Period number badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(statusColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "P${slot.periodNumber}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor,
            )
        }
    }
}

/**
 * Which subjects the caller is attached to.
 *
 * For a teacher that is the subjects they are assigned to teach, with how many
 * periods each has on the grid — "not timetabled yet" is worth surfacing, because
 * an assignment nobody scheduled is invisible otherwise. For a student it is the
 * subjects of their class and who teaches each one.
 */
@Composable
private fun MySubjectsTab(
    mappings: List<ClassSubjectTeacherDto>,
    slots: List<TimetableSlotDto>,
    isTeacher: Boolean,
) {
    if (mappings.isEmpty()) {
        EmptyView(
            title = "Nothing assigned yet",
            message = if (isTeacher) {
                "Subjects you are assigned to teach appear here."
            } else {
                "Subject teachers have not been assigned for your class yet."
            },
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(mappings.size, key = { mappings[it].id }) { index ->
            val mapping = mappings[index]
            val periods = slots.count { it.subjectId == mapping.subjectId }
            EntityRowCard(
                title = mapping.subjectName ?: "Subject #${mapping.subjectId}",
                subtitle = if (isTeacher) {
                    mapping.className
                } else {
                    mapping.teacherName ?: "Unassigned"
                },
                metadata = if (periods > 0) {
                    "$periods period${if (periods == 1) "" else "s"} a week"
                } else {
                    "Not timetabled yet"
                },
                leadingInitials = (mapping.subjectName ?: "?").take(1),
            )
        }
    }
}

@HiltViewModel
class MyTimetableViewModel @Inject constructor(
    private val academicRepository: AcademicRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val role = Role.from(sessionManager.currentUser?.role)

    private val _state = MutableStateFlow(MyTimetableUiState(isTeacher = role.isTeaching))
    val state: StateFlow<MyTimetableUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            coroutineScope {
                // Fetched together: the subjects tab counts periods from the week, so
                // showing one without the other would report "not timetabled" wrongly.
                val slots = async { academicRepository.getMyTimetable() }
                val mappings = async { academicRepository.getMyTeacherMappings() }

                val slotResult = slots.await()
                _state.value = MyTimetableUiState(
                    isTeacher = role.isTeaching,
                    slots = slotResult.getOrNull().orEmpty(),
                    mappings = mappings.await().getOrNull().orEmpty(),
                    isLoading = false,
                    // Only the week's failure is worth reporting: the subject list is
                    // supplementary, and a role with no timetable of its own (an office
                    // login that reached this screen) is told by the empty state.
                    error = (slotResult as? com.greenwood.school.core.network.ApiResult.Failure)?.error,
                )
            }
        }
    }
}

data class MyTimetableUiState(
    val isTeacher: Boolean = false,
    val slots: List<TimetableSlotDto> = emptyList(),
    val mappings: List<ClassSubjectTeacherDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
)

package com.greenwood.school.ui.feature.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.ui.components.AppCard
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.DateField
import com.greenwood.school.ui.components.DetailRow
import com.greenwood.school.ui.components.DropdownField
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.LoadMoreFooter
import com.greenwood.school.ui.components.SectionCard
import com.greenwood.school.ui.components.StatusChip
import com.greenwood.school.ui.theme.StatusPalette

/**
 * Attendance, as four tabs rather than the web app's four routes.
 *
 * The web marking grid is a table with a radio column per status. On a phone each
 * student is a card with a row of one-letter status buttons — the same five
 * choices, thumb-sized, with no horizontal scrolling.
 */
@Composable
fun AttendanceScreen(
    onBack: (() -> Unit)? = null,
    viewModel: AttendanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val report by viewModel.report.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs = remember(state.canMark) {
        if (state.canMark) listOf("Mark", "Register", "Monthly", "Staff") else listOf("My attendance")
    }
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.message?.id) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Attendance", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (tabs.size > 1) {
                ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = tab == index,
                            onClick = {
                                tab = index
                                when (index) {
                                    2 -> viewModel.loadMonthly()
                                    3 -> viewModel.loadTeacherGrid()
                                }
                            },
                            text = { Text(label) },
                        )
                    }
                }
            }

            if (!state.canMark) {
                SelfServiceTab(state = state, report = report, viewModel = viewModel)
                return@Column
            }

            when (tab) {
                0 -> MarkTab(state, viewModel)
                1 -> RegisterTab(state, report, viewModel)
                2 -> MonthlyTab(state, viewModel)
                else -> StaffTab(state, viewModel)
            }
        }
    }
}

/* ---- Mark students ------------------------------------------------------- */

@Composable
private fun MarkTab(state: AttendanceUiState, viewModel: AttendanceViewModel) {
    Column(Modifier.fillMaxSize()) {
        ClassSectionDatePicker(state, viewModel)

        when {
            state.selectedSection == null -> EmptyView(
                title = "Pick a class and section",
                message = "Choose a class, section and date to load the register.",
            )

            state.isGridLoading -> FullScreenLoader()

            state.gridError != null -> ErrorView(error = state.gridError, onRetry = viewModel::loadGrid)

            state.grid.isEmpty() -> EmptyView(
                title = "No students",
                message = "This section has no active students.",
            )

            else -> Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${state.markedCount} of ${state.grid.size} marked",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    // The overwhelmingly common case is "all present, then fix a few".
                    TextButton(onClick = { viewModel.markAllStudents("PRESENT") }) { Text("All present") }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.grid.size, key = { state.grid[it].studentId }) { index ->
                        val row = state.grid[index]
                        AppCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(row.displayName, style = MaterialTheme.typography.titleSmall)
                                row.rollNumber?.let {
                                    Text(
                                        "Roll no. $it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                StatusSelector(
                                    selected = state.marks[row.studentId],
                                    onSelect = { viewModel.setStudentStatus(row.studentId, it) },
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = viewModel::saveStudentAttendance,
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth().padding(12.dp).height(48.dp),
                ) { Text(if (state.isSaving) "Saving…" else "Save attendance") }
            }
        }
    }
}

/**
 * The five backend statuses as a compact segmented row.
 *
 * Single letters (P/A/L/H/Lv) rather than full words: five full labels do not fit
 * a phone width, and the colour coding carries the meaning at a glance.
 */
@Composable
private fun StatusSelector(selected: String?, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Constants.ATTENDANCE_STATUSES.forEach { status ->
            val isSelected = selected == status
            val color = StatusPalette.forStatus(status)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) color else color.copy(alpha = 0.12f))
                    .clickable { onSelect(status) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = status.abbreviate(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else color,
                )
            }
        }
    }
}

private fun String.abbreviate(): String = when (this) {
    "PRESENT" -> "P"
    "ABSENT" -> "A"
    "LATE" -> "L"
    "HALF_DAY" -> "H"
    "LEAVE" -> "Lv"
    else -> take(1)
}

/* ---- Register (paged report) ---------------------------------------------- */

@Composable
private fun RegisterTab(
    state: AttendanceUiState,
    report: com.greenwood.school.ui.common.PagedListState<
        com.greenwood.school.data.remote.dto.StudentAttendanceReportRowDto,
        >,
    viewModel: AttendanceViewModel,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Box(Modifier.weight(1f)) {
                DateField(
                    label = "From",
                    value = state.rangeStart,
                    onValueChange = { viewModel.onRangeSelected(it, state.rangeEnd) },
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                DateField(
                    label = "To",
                    value = state.rangeEnd,
                    onValueChange = { viewModel.onRangeSelected(state.rangeStart, it) },
                )
            }
        }

        when {
            report.isInitialLoad -> FullScreenLoader()
            report.error != null -> ErrorView(error = report.error, onRetry = viewModel::refreshReport)
            report.isEmpty -> EmptyView(
                title = "No attendance recorded",
                message = "Nothing was marked in this date range.",
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(report.items.size, key = { report.items[it].id }) { index ->
                    val row = report.items[index]
                    EntityRowCard(
                        title = row.displayName,
                        subtitle = Formatters.date(row.attendanceDate),
                        metadata = listOfNotNull(row.className, row.sectionName)
                            .joinToString(" - ")
                            .ifBlank { null },
                        leadingInitials = Formatters.initials(row.displayName),
                        trailing = { StatusChip(row.status) },
                    )
                }
                item {
                    LoadMoreFooter(report.isAppending, report.appendError, viewModel::loadMoreReport)
                }
            }
        }
    }
}

/* ---- Monthly matrix -------------------------------------------------------- */

/**
 * The one place a horizontal scroll is justified: a month-by-day grid genuinely
 * has 31 columns and collapsing it would destroy the thing being looked at. Names
 * stay in a fixed left column so the row is never orphaned from its student.
 */
@Composable
private fun MonthlyTab(state: AttendanceUiState, viewModel: AttendanceViewModel) {
    Column(Modifier.fillMaxSize()) {
        ClassSectionDatePicker(state, viewModel, showDate = false)

        when {
            state.selectedSection == null -> EmptyView(
                title = "Pick a class and section",
                message = "Choose a class and section to see the monthly register.",
            )

            state.isMonthlyLoading -> FullScreenLoader()

            state.monthly.isEmpty() -> EmptyView(title = "Nothing recorded this month")

            else -> {
                val scroll = rememberScrollState()
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item {
                        Text(
                            Formatters.monthYear(state.month, state.monthYear),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    items(state.monthly.size, key = { state.monthly[it].studentId }) { index ->
                        val row = state.monthly[index]
                        AppCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text(row.displayName, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(scroll),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    (1..31).forEach { day ->
                                        val status = row.days[day.toString()]
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                day.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (status == null) {
                                                            MaterialTheme.colorScheme.surfaceVariant
                                                        } else {
                                                            StatusPalette.forStatus(status).copy(alpha = 0.85f)
                                                        },
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---- Staff attendance -------------------------------------------------------- */

@Composable
private fun StaffTab(state: AttendanceUiState, viewModel: AttendanceViewModel) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            DateField(label = "Date", value = state.date, onValueChange = viewModel::onDateSelected)
        }

        when {
            state.isTeacherGridLoading -> FullScreenLoader()
            state.teacherGrid.isEmpty() -> EmptyView(title = "No staff to mark")
            else -> Column(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.teacherGrid.size, key = { state.teacherGrid[it].teacherId }) { index ->
                        val row = state.teacherGrid[index]
                        AppCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(row.displayName, style = MaterialTheme.typography.titleSmall)
                                row.employeeId?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                StatusSelector(
                                    selected = state.teacherMarks[row.teacherId],
                                    onSelect = { viewModel.setTeacherStatus(row.teacherId, it) },
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = viewModel::saveTeacherAttendance,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().padding(12.dp).height(48.dp),
                ) { Text(if (state.isSaving) "Saving…" else "Save staff attendance") }
            }
        }
    }
}

/* ---- Student / parent view ------------------------------------------------------ */

@Composable
private fun SelfServiceTab(
    state: AttendanceUiState,
    report: com.greenwood.school.ui.common.PagedListState<
        com.greenwood.school.data.remote.dto.StudentAttendanceReportRowDto,
        >,
    viewModel: AttendanceViewModel,
) {
    Column(Modifier.fillMaxSize()) {
        state.ownSummary?.let { summary ->
            SectionCard(title = "This year", modifier = Modifier.padding(12.dp)) {
                DetailRow("Attendance", Formatters.percent(summary.percentage))
                DetailRow("Present", summary.presentDays.toString())
                DetailRow("Absent", summary.absentDays.toString())
                DetailRow("Late", summary.lateDays.toString())
                DetailRow("Half days", summary.halfDays.toString())
                DetailRow("On leave", summary.leaveDays.toString())
                DetailRow("Days marked", summary.totalMarkedDays.toString())
            }
        }
        RegisterTab(state, report, viewModel)
    }
}

/* ---- Shared pickers ------------------------------------------------------------- */

@Composable
private fun ClassSectionDatePicker(
    state: AttendanceUiState,
    viewModel: AttendanceViewModel,
    showDate: Boolean = true,
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Class only: with one section per class there was nothing to pick, so the
        // section is resolved from the class in the view model instead.
        DropdownField(
            label = "Class",
            options = state.classes,
            selected = state.selectedClass,
            onSelected = viewModel::onClassSelected,
            optionLabel = { it.className },
        )
        if (showDate) {
            Spacer(Modifier.height(8.dp))
            DateField(label = "Date", value = state.date, onValueChange = viewModel::onDateSelected)
        }
    }
}

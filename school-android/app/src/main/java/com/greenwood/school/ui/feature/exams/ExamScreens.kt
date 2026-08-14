package com.greenwood.school.ui.feature.exams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.isManagement
import com.greenwood.school.core.common.isTeaching
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.dto.ExamDto
import com.greenwood.school.data.remote.dto.ExamResultRowDto
import com.greenwood.school.data.remote.dto.ExamScheduleDto
import com.greenwood.school.data.remote.dto.MarkRosterRowDto
import com.greenwood.school.data.remote.dto.MarksEntryRecordDto
import com.greenwood.school.domain.repository.ExamRepository
import com.greenwood.school.navigation.Routes
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.common.UiMessage
import com.greenwood.school.ui.components.AppCard
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.PagedListScaffold
import com.greenwood.school.ui.components.SectionCard
import com.greenwood.school.ui.components.DetailRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ------------------------------------------------------------------------- */
/* Exam list                                                                  */
/* ------------------------------------------------------------------------- */

@Composable
fun ExamListScreen(
    onOpenExam: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ExamListViewModel = hiltViewModel(),
) {
    val list by viewModel.list.collectAsStateWithLifecycle()

    PagedListScaffold(
        title = "Exams & Marks",
        state = list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        emptyTitle = "No exams scheduled",
        key = { it.id },
    ) { exam ->
        EntityRowCard(
            title = exam.title,
            subtitle = "${Formatters.date(exam.startDate)} → ${Formatters.date(exam.endDate)}",
            metadata = exam.academicYearName,
            leadingInitials = Formatters.initials(exam.examTypeName ?: "Exam"),
            showChevron = true,
            onClick = { onOpenExam(exam.id) },
        )
    }
}

@HiltViewModel
class ExamListViewModel @Inject constructor(
    private val examRepository: ExamRepository,
) : ViewModel() {

    private val loader = PagedLoader(viewModelScope) { page, size ->
        examRepository.getExams(page = page, size = size)
    }
    val list = loader.state

    init {
        loader.refresh()
    }

    fun refresh() = loader.refresh()
    fun loadMore() = loader.loadMore()
}

/* ------------------------------------------------------------------------- */
/* Exam detail — schedules and results                                        */
/* ------------------------------------------------------------------------- */

@Composable
fun ExamDetailScreen(
    onOpenMarksEntry: (examId: Long, scheduleId: Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ExamDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = state.exam?.title ?: "Exam",
                subtitle = state.exam?.let {
                    "${Formatters.date(it.startDate)} → ${Formatters.date(it.endDate)}"
                },
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                listOf("Schedule", "Results").forEachIndexed { index, label ->
                    Tab(
                        selected = tab == index,
                        onClick = {
                            tab = index
                            if (index == 1) viewModel.loadResults()
                        },
                        text = { Text(label) },
                    )
                }
            }

            when {
                state.isLoading -> FullScreenLoader()
                state.error != null -> ErrorView(error = state.error!!, onRetry = viewModel::load)
                tab == 0 -> ScheduleTab(
                    schedules = state.schedules,
                    canEnterMarks = state.canEnterMarks,
                    onEnterMarks = { scheduleId -> onOpenMarksEntry(state.examId, scheduleId) },
                )

                else -> ResultsTab(state.results, state.isResultsLoading)
            }
        }
    }
}

@Composable
private fun ScheduleTab(
    schedules: List<ExamScheduleDto>,
    canEnterMarks: Boolean,
    onEnterMarks: (Long) -> Unit,
) {
    if (schedules.isEmpty()) {
        EmptyView(title = "No papers scheduled", message = "Subjects appear here once the timetable is set.")
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(schedules.size, key = { schedules[it].id }) { index ->
            val schedule = schedules[index]
            EntityRowCard(
                title = schedule.subjectName ?: "Subject",
                subtitle = "${Formatters.date(schedule.examDate)} · " +
                    "${Formatters.time(schedule.startTime)} – ${Formatters.time(schedule.endTime)}",
                metadata = listOfNotNull(
                    "Max ${schedule.maxMarks.toInt()} marks",
                    schedule.roomNumber?.let { "Room $it" },
                ).joinToString(" · "),
                leadingInitials = Formatters.initials(schedule.subjectName.orEmpty()),
                trailing = {
                    if (canEnterMarks) {
                        TextButton(onClick = { onEnterMarks(schedule.id) }) { Text("Marks") }
                    }
                },
            )
        }
    }
}

@Composable
private fun ResultsTab(results: List<ExamResultRowDto>, isLoading: Boolean) {
    when {
        isLoading -> FullScreenLoader()
        results.isEmpty() -> EmptyView(
            title = "No results yet",
            message = "Results appear once marks have been entered for every paper.",
        )

        else -> LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(results.size, key = { results[it].studentId }) { index ->
                val row = results[index]
                EntityRowCard(
                    // Rank is the first thing anyone looks for on a results list, so it
                    // takes the avatar slot rather than hiding in the metadata line.
                    title = row.studentName,
                    subtitle = "Roll no. ${row.rollNumber ?: Formatters.PLACEHOLDER}",
                    metadata = "${row.totalObtained.toInt()} / ${row.totalMax.toInt()}",
                    leadingInitials = "#${row.rank}",
                    trailing = {
                        Text(
                            Formatters.percent(row.percentage),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                )
            }
        }
    }
}

@HiltViewModel
class ExamDetailViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val examId: Long = checkNotNull(savedStateHandle[Routes.ARG_EXAM_ID])
    private val role = Role.from(sessionManager.currentUser?.role)

    private val _state = MutableStateFlow(
        ExamDetailUiState(examId = examId, canEnterMarks = role.isManagement || role.isTeaching),
    )
    val state: StateFlow<ExamDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            coroutineScope {
                val exam = async { examRepository.getExam(examId) }
                val schedules = async { examRepository.getSchedules(examId).getOrNull().orEmpty() }

                when (val examResult = exam.await()) {
                    is ApiResult.Success -> _state.update {
                        it.copy(isLoading = false, exam = examResult.data, schedules = schedules.await())
                    }

                    is ApiResult.Failure -> _state.update {
                        it.copy(isLoading = false, error = examResult.error)
                    }
                }
            }
        }
    }

    fun loadResults() {
        if (_state.value.results.isNotEmpty()) return
        _state.update { it.copy(isResultsLoading = true) }
        viewModelScope.launch {
            val results = examRepository.getResults(examId).getOrNull().orEmpty()
            _state.update { it.copy(isResultsLoading = false, results = results) }
        }
    }
}

data class ExamDetailUiState(
    val examId: Long,
    val canEnterMarks: Boolean = false,
    val exam: ExamDto? = null,
    val schedules: List<ExamScheduleDto> = emptyList(),
    val results: List<ExamResultRowDto> = emptyList(),
    val isLoading: Boolean = true,
    val isResultsLoading: Boolean = false,
    val error: AppError? = null,
)

/* ------------------------------------------------------------------------- */
/* Marks entry                                                                */
/* ------------------------------------------------------------------------- */

/**
 * The marks grid.
 *
 * `/marks/roster` returns every active student in the class with their mark or
 * null, so the teacher always sees the full list and can tell "not entered" from
 * "scored zero" — a distinction the plain `/marks` list would lose.
 */
@Composable
fun MarksEntryScreen(
    onBack: () -> Unit,
    viewModel: MarksEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(state.message?.id) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Enter marks", subtitle = state.subtitle, onBack = onBack) },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> FullScreenLoader()
                state.error != null -> ErrorView(error = state.error!!, onRetry = viewModel::load)
                state.roster.isEmpty() -> EmptyView(title = "No students in this class")
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.roster.size, key = { state.roster[it].studentId }) { index ->
                            MarkRow(
                                row = state.roster[index],
                                value = state.entries[state.roster[index].studentId].orEmpty(),
                                error = state.errors[state.roster[index].studentId],
                                onChange = { viewModel.setMark(state.roster[index].studentId, it) },
                            )
                        }
                    }
                    Button(
                        onClick = viewModel::save,
                        enabled = !state.isSaving && state.enteredCount > 0,
                        modifier = Modifier.fillMaxWidth().padding(12.dp).height(48.dp),
                    ) {
                        Text(
                            if (state.isSaving) {
                                "Saving…"
                            } else {
                                "Save ${state.enteredCount} mark${if (state.enteredCount == 1) "" else "s"}"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkRow(
    row: MarkRosterRowDto,
    value: String,
    error: String?,
    onChange: (String) -> Unit,
) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOfNotNull(
                        row.rollNumber?.let { "Roll no. $it" },
                        row.sectionName?.let { "Section $it" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.width(110.dp),
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                label = { Text("/ ${row.maxMarks.toInt()}") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                ),
            )
        }
    }
}

@HiltViewModel
class MarksEntryViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val scheduleId: Long = checkNotNull(savedStateHandle[Routes.ARG_SCHEDULE_ID])

    private val _state = MutableStateFlow(MarksEntryUiState())
    val state: StateFlow<MarksEntryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = examRepository.getMarksRoster(scheduleId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        roster = result.data,
                        // Pre-fill anything already entered so a teacher can correct a
                        // mark instead of retyping the whole class.
                        entries = result.data
                            .mapNotNull { row -> row.marksObtained?.let { m -> row.studentId to trimZero(m) } }
                            .toMap(),
                        subtitle = result.data.firstOrNull()?.sectionName?.let { "Section $it" },
                    )
                }

                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    fun setMark(studentId: Long, value: String) = _state.update { current ->
        val row = current.roster.firstOrNull { it.studentId == studentId }
        val parsed = value.toDoubleOrNull()
        val error = when {
            value.isBlank() -> null
            parsed == null -> "Numbers only"
            parsed < 0 -> "Cannot be negative"
            row != null && parsed > row.maxMarks -> "Max ${row.maxMarks.toInt()}"
            else -> null
        }
        current.copy(
            entries = if (value.isBlank()) current.entries - studentId else current.entries + (studentId to value),
            errors = if (error == null) current.errors - studentId else current.errors + (studentId to error),
        )
    }

    fun save() {
        val current = _state.value
        if (current.errors.isNotEmpty()) {
            _state.update { it.copy(message = UiMessage("Fix the highlighted marks first.", isError = true)) }
            return
        }

        val records = current.entries.mapNotNull { (studentId, raw) ->
            raw.toDoubleOrNull()?.let { MarksEntryRecordDto(studentId = studentId, marksObtained = it) }
        }
        if (records.isEmpty()) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = examRepository.saveMarks(scheduleId, records)
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        isSaving = false,
                        message = UiMessage.success("Saved ${records.size} marks."),
                    )

                    is ApiResult.Failure -> it.copy(isSaving = false, message = UiMessage.error(result.error))
                }
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    /** 47.0 → "47" so a whole-number mark doesn't render with a pointless decimal. */
    private fun trimZero(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}

data class MarksEntryUiState(
    val roster: List<MarkRosterRowDto> = emptyList(),
    val entries: Map<Long, String> = emptyMap(),
    val errors: Map<Long, String> = emptyMap(),
    val subtitle: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: AppError? = null,
    val message: UiMessage? = null,
) {
    val enteredCount: Int get() = entries.count { it.value.isNotBlank() }
}

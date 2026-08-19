package com.greenwood.school.ui.feature.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                        ClassTimetableTab(state.slots)
                    } else {
                        MySubjectsTab(state.mappings, state.slots, state.isTeacher)
                    }
                }
            }
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

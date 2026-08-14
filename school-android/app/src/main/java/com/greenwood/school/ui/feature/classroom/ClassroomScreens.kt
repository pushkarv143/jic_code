package com.greenwood.school.ui.feature.classroom

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.isSelfService
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.dto.AssignmentDto
import com.greenwood.school.data.remote.dto.AssignmentSubmissionDto
import com.greenwood.school.domain.repository.ClassroomRepository
import com.greenwood.school.navigation.Routes
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.DetailRow
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.PagedListScaffold
import com.greenwood.school.ui.components.SectionCard
import com.greenwood.school.ui.components.StatusChip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/* ------------------------------------------------------------------------- */
/* Assignments                                                                */
/* ------------------------------------------------------------------------- */

@Composable
fun AssignmentListScreen(
    onOpenAssignment: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: AssignmentListViewModel = hiltViewModel(),
) {
    val list by viewModel.list.collectAsStateWithLifecycle()

    PagedListScaffold(
        title = "Assignments",
        state = list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        emptyTitle = "No assignments",
        emptyMessage = "Assignments set by your teachers appear here.",
        key = { it.id },
    ) { assignment ->
        EntityRowCard(
            title = assignment.title,
            subtitle = listOfNotNull(assignment.subjectName, assignment.className, assignment.sectionName)
                .joinToString(" · ")
                .ifBlank { null },
            metadata = "Due ${Formatters.date(assignment.dueDate)}",
            leadingInitials = Formatters.initials(assignment.subjectName ?: assignment.title),
            trailing = { OverdueChip(assignment) },
            showChevron = true,
            onClick = { onOpenAssignment(assignment.id) },
        )
    }
}

/** Past-due assignments are the ones a student needs to spot instantly. */
@Composable
private fun OverdueChip(assignment: AssignmentDto) {
    val due = Formatters.parseDate(assignment.dueDate) ?: return
    if (due.isBefore(LocalDate.now())) StatusChip("OVERDUE")
}

@HiltViewModel
class AssignmentListViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val user = sessionManager.currentUser
    private val role = Role.from(user?.role)

    private val loader = PagedLoader(viewModelScope) { page, size ->
        classroomRepository.getAssignments(
            // A student sees their own class/section; a teacher sees what they set.
            classId = user?.classId.takeIf { role.isSelfService },
            sectionId = user?.sectionId.takeIf { role.isSelfService },
            teacherId = user?.teacherId.takeIf { role == Role.TEACHER || role == Role.CLASS_TEACHER },
            page = page,
            size = size,
        )
    }
    val list = loader.state

    init {
        loader.refresh()
    }

    fun refresh() = loader.refresh()
    fun loadMore() = loader.loadMore()
}

/* ------------------------------------------------------------------------- */
/* Assignment detail — description, attachment, submissions                   */
/* ------------------------------------------------------------------------- */

@Composable
fun AssignmentDetailScreen(
    onBack: () -> Unit,
    viewModel: AssignmentDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = { AppTopBar(title = state.assignment?.title ?: "Assignment", onBack = onBack) },
    ) { padding ->
        when {
            state.isLoading -> FullScreenLoader()
            state.error != null -> ErrorView(error = state.error!!, onRetry = viewModel::load)
            state.assignment == null -> EmptyView(title = "Assignment not found")
            else -> {
                val assignment = state.assignment!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionCard(title = "Details") {
                        DetailRow("Subject", assignment.subjectName)
                        DetailRow("Class", listOfNotNull(assignment.className, assignment.sectionName)
                            .joinToString(" - ").ifBlank { null })
                        DetailRow("Set by", assignment.teacherName)
                        DetailRow("Assigned", Formatters.date(assignment.assignedDate))
                        DetailRow("Due", Formatters.date(assignment.dueDate))
                    }

                    if (!assignment.description.isNullOrBlank()) {
                        SectionCard(title = "Instructions") {
                            Text(assignment.description!!, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    assignment.fileUrl?.let { url ->
                        SectionCard(title = "Attachment") {
                            TextButton(
                                onClick = {
                                    // Attachment URLs are served by the backend's /uploads
                                    // handler; hand them to the browser rather than trying
                                    // to render arbitrary file types in-app.
                                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                                },
                            ) { Text("Open attachment") }
                        }
                    }

                    state.mySubmission?.let { submission ->
                        SectionCard(title = "My submission") {
                            DetailRow("Submitted", Formatters.dateTime(submission.submittedAt))
                            DetailRow("Status", Formatters.humanizeEnum(submission.status))
                            DetailRow("Marks", submission.marksObtained?.toString())
                            DetailRow("Feedback", submission.feedback)
                        }
                    }

                    if (state.submissions.isNotEmpty()) {
                        SectionCard(title = "Submissions (${state.submissions.size})") {
                            state.submissions.forEach { submission -> SubmissionRow(submission) }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SubmissionRow(submission: AssignmentSubmissionDto) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            submission.studentName ?: "Student #${submission.studentId}",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            listOfNotNull(
                submission.rollNumber?.let { "Roll no. $it" },
                Formatters.dateTime(submission.submittedAt),
                submission.marksObtained?.let { "Marks $it" },
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@HiltViewModel
class AssignmentDetailViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val assignmentId: Long = checkNotNull(savedStateHandle[Routes.ARG_ASSIGNMENT_ID])
    private val role = Role.from(sessionManager.currentUser?.role)

    private val _state = MutableStateFlow(AssignmentDetailUiState())
    val state: StateFlow<AssignmentDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            coroutineScope {
                val assignment = async { classroomRepository.getAssignment(assignmentId) }

                // A student may only read their own submission; the roster call is for
                // staff. Asking for the wrong one just produces a 403, so branch on role.
                val mine = if (role.isSelfService) {
                    async { classroomRepository.getMySubmission(assignmentId).getOrNull() }
                } else null

                val all = if (!role.isSelfService) {
                    async { classroomRepository.getSubmissions(assignmentId).getOrNull().orEmpty() }
                } else null

                when (val result = assignment.await()) {
                    is ApiResult.Success -> _state.update {
                        it.copy(
                            isLoading = false,
                            assignment = result.data,
                            mySubmission = mine?.await(),
                            submissions = all?.await().orEmpty(),
                        )
                    }

                    is ApiResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
                }
            }
        }
    }
}

data class AssignmentDetailUiState(
    val assignment: AssignmentDto? = null,
    val mySubmission: AssignmentSubmissionDto? = null,
    val submissions: List<AssignmentSubmissionDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
)

/* ------------------------------------------------------------------------- */
/* Online classes                                                             */
/* ------------------------------------------------------------------------- */

@Composable
fun OnlineClassesScreen(
    onBack: (() -> Unit)? = null,
    viewModel: OnlineClassesViewModel = hiltViewModel(),
) {
    val list by viewModel.list.collectAsStateWithLifecycle()
    val context = LocalContext.current

    PagedListScaffold(
        title = "Online Classes",
        state = list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        emptyTitle = "No online classes scheduled",
        key = { it.id },
    ) { onlineClass ->
        EntityRowCard(
            title = onlineClass.title,
            subtitle = listOfNotNull(onlineClass.subjectName, onlineClass.teacherName)
                .joinToString(" · ")
                .ifBlank { null },
            metadata = "${Formatters.dateTime(onlineClass.scheduledAt)} · ${onlineClass.durationMinutes} min",
            leadingInitials = Formatters.initials(onlineClass.subjectName ?: onlineClass.title),
            trailing = {
                TextButton(
                    onClick = {
                        // Meeting links are third-party (Meet/Zoom/Teams); handing them to
                        // the system chooser lets the installed app take over if present.
                        context.startActivity(Intent(Intent.ACTION_VIEW, onlineClass.meetingLink.toUri()))
                    },
                    enabled = onlineClass.meetingLink.isNotBlank(),
                ) { Text("Join") }
            },
        )
    }
}

@HiltViewModel
class OnlineClassesViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val user = sessionManager.currentUser
    private val role = Role.from(user?.role)

    private val loader = PagedLoader(viewModelScope) { page, size ->
        classroomRepository.getOnlineClasses(
            classId = user?.classId.takeIf { role.isSelfService },
            sectionId = user?.sectionId.takeIf { role.isSelfService },
            // Past sessions are noise on a phone; the web page defaults the same way.
            upcoming = true,
            page = page,
            size = size,
        )
    }
    val list = loader.state

    init {
        loader.refresh()
    }

    fun refresh() = loader.refresh()
    fun loadMore() = loader.loadMore()
}

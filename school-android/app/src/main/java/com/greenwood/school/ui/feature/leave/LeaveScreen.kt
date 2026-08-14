package com.greenwood.school.ui.feature.leave

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.isManagement
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.dto.LeaveApplicationDto
import com.greenwood.school.data.remote.dto.LeaveApplicationRequestDto
import com.greenwood.school.domain.repository.AttendanceRepository
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.common.UiMessage
import com.greenwood.school.ui.components.AppTextField
import com.greenwood.school.ui.components.DateField
import com.greenwood.school.ui.components.EnumDropdownField
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.FilterChipRow
import com.greenwood.school.ui.components.PagedListScaffold
import com.greenwood.school.ui.components.StatusChip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Leave applications.
 *
 * Two tabs where the role allows it: "My leave" (everyone) and "All applications"
 * (approvers only). Applying is a bottom sheet rather than a screen — it's four
 * fields and returning to the list afterwards should be free.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveScreen(
    onBack: (() -> Unit)? = null,
    viewModel: LeaveViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val myLeave by viewModel.myLeave.collectAsStateWithLifecycle()
    val allLeave by viewModel.allLeave.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showApply by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val showsAllTab = state.canApprove
    val list = if (tab == 0 || !showsAllTab) myLeave else allLeave

    PagedListScaffold(
        title = "Leave",
        state = list,
        onRefresh = { if (tab == 0) viewModel.refreshMine() else viewModel.refreshAll() },
        onLoadMore = { if (tab == 0) viewModel.loadMoreMine() else viewModel.loadMoreAll() },
        onBack = onBack,
        hasActiveFilters = state.statusFilter != null,
        message = state.message,
        onMessageShown = viewModel::consumeMessage,
        emptyTitle = if (tab == 0) "No leave applications" else "Nothing to review",
        emptyMessage = if (tab == 0) "Apply for leave with the + button." else null,
        onAdd = { showApply = true },
        addContentDescription = "Apply for leave",
        key = { it.id },
        header = if (showsAllTab) {
            {
                TabRow(selectedTabIndex = tab) {
                    listOf("My leave", "All applications").forEachIndexed { index, label ->
                        Tab(
                            selected = tab == index,
                            onClick = {
                                tab = index
                                if (index == 1) viewModel.refreshAll()
                            },
                            text = { Text(label) },
                        )
                    }
                }
            }
        } else {
            null
        },
        filterSheet = if (tab == 1) {
            { dismiss ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Filter by status", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    FilterChipRow(
                        options = Constants.LEAVE_STATUSES,
                        selected = state.statusFilter,
                        onSelected = viewModel::onStatusFilter,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                    Spacer(Modifier.height(16.dp))
                }
            }
        } else {
            null
        },
    ) { application ->
        LeaveRow(
            application = application,
            // Approve/reject only appears on the review tab, and only while pending.
            canReview = state.canApprove && tab == 1 && application.status == "PENDING",
            onApprove = { viewModel.approve(application) },
            onReject = { viewModel.reject(application) },
        )
    }

    if (showApply) {
        ModalBottomSheet(onDismissRequest = { showApply = false }, sheetState = sheetState) {
            ApplyLeaveSheet(
                isSubmitting = state.isSubmitting,
                onSubmit = { type, start, end, reason ->
                    viewModel.apply(type, start, end, reason) { showApply = false }
                },
            )
        }
    }
}

@Composable
private fun LeaveRow(
    application: LeaveApplicationDto,
    canReview: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    EntityRowCard(
        title = application.applicantName ?: Formatters.humanizeEnum(application.leaveType),
        subtitle = "${Formatters.date(application.startDate)} → ${Formatters.date(application.endDate)}",
        metadata = listOfNotNull(
            Formatters.humanizeEnum(application.leaveType).takeIf { application.applicantName != null },
            application.reason.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifBlank { null },
        leadingInitials = Formatters.initials(
            application.applicantName ?: application.leaveType,
        ),
        trailing = {
            if (canReview) {
                Row {
                    IconButton(onClick = onApprove) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = "Approve",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onReject) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Reject",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                StatusChip(application.status)
            }
        },
    )
}

@Composable
private fun ApplyLeaveSheet(
    isSubmitting: Boolean,
    onSubmit: (type: String, start: LocalDate, end: LocalDate, reason: String) -> Unit,
) {
    var type by remember { mutableStateOf(Constants.LEAVE_TYPES.first()) }
    var start by remember { mutableStateOf(LocalDate.now()) }
    var end by remember { mutableStateOf(LocalDate.now()) }
    var reason by remember { mutableStateOf("") }
    var reasonError by remember { mutableStateOf<String?>(null) }
    var rangeError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Apply for leave", style = MaterialTheme.typography.titleMedium)

        EnumDropdownField(
            label = "Leave type",
            options = Constants.LEAVE_TYPES,
            selected = type,
            onSelected = { type = it },
            required = true,
        )

        Row {
            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                DateField(label = "From", value = start, onValueChange = { start = it; rangeError = null }, required = true)
            }
            Spacer(Modifier.padding(horizontal = 5.dp))
            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                DateField(
                    label = "To",
                    value = end,
                    onValueChange = { end = it; rangeError = null },
                    required = true,
                    error = rangeError,
                )
            }
        }

        AppTextField(
            value = reason,
            onValueChange = { reason = it; reasonError = null },
            label = "Reason",
            required = true,
            error = reasonError,
            singleLine = false,
            minLines = 3,
        )

        Button(
            onClick = {
                // Validate here rather than server-side round trip: an end date before
                // the start is the one mistake people actually make on this form.
                reasonError = if (reason.isBlank()) "Reason is required" else null
                rangeError = if (end.isBefore(start)) "End date cannot be before the start date" else null
                if (reasonError == null && rangeError == null) onSubmit(type, start, end, reason)
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(if (isSubmitting) "Submitting…" else "Submit application") }

        Spacer(Modifier.height(16.dp))
    }
}

@HiltViewModel
class LeaveViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val role = Role.from(sessionManager.currentUser?.role)

    private val _state = MutableStateFlow(LeaveUiState(canApprove = role.isManagement))
    val state: StateFlow<LeaveUiState> = _state.asStateFlow()

    private val mineLoader = PagedLoader(viewModelScope) { page, size ->
        attendanceRepository.getMyLeaveApplications(page, size)
    }
    val myLeave = mineLoader.state

    private val allLoader = PagedLoader(viewModelScope) { page, size ->
        attendanceRepository.getLeaveApplications(status = _state.value.statusFilter, page = page, size = size)
    }
    val allLeave = allLoader.state

    init {
        mineLoader.refresh()
        if (role.isManagement) allLoader.refresh()
    }

    fun onStatusFilter(status: String?) {
        _state.update { it.copy(statusFilter = status) }
        allLoader.refresh()
    }

    fun apply(type: String, start: LocalDate, end: LocalDate, reason: String, onDone: () -> Unit) {
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = attendanceRepository.applyForLeave(
                LeaveApplicationRequestDto(
                    leaveType = type,
                    startDate = Formatters.apiDate(start),
                    endDate = Formatters.apiDate(end),
                    reason = reason.trim(),
                ),
            )
            when (result) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(isSubmitting = false, message = UiMessage.success("Leave application submitted."))
                    }
                    mineLoader.refresh()
                    onDone()
                }

                is ApiResult.Failure ->
                    _state.update { it.copy(isSubmitting = false, message = UiMessage.error(result.error)) }
            }
        }
    }

    fun approve(application: LeaveApplicationDto) = review(application, approve = true)

    fun reject(application: LeaveApplicationDto) = review(application, approve = false)

    private fun review(application: LeaveApplicationDto, approve: Boolean) {
        viewModelScope.launch {
            val result = if (approve) {
                attendanceRepository.approveLeave(application.id)
            } else {
                attendanceRepository.rejectLeave(application.id)
            }
            when (result) {
                is ApiResult.Success -> {
                    // Swap the row in place so the decision is visible immediately.
                    allLoader.replaceWhere({ it.id == application.id }, result.data)
                    _state.update {
                        it.copy(
                            message = UiMessage.success(
                                if (approve) "Leave approved." else "Leave rejected.",
                            ),
                        )
                    }
                }

                is ApiResult.Failure -> _state.update { it.copy(message = UiMessage.error(result.error)) }
            }
        }
    }

    fun refreshMine() = mineLoader.refresh()
    fun loadMoreMine() = mineLoader.loadMore()
    fun refreshAll() = allLoader.refresh()
    fun loadMoreAll() = allLoader.loadMore()
    fun consumeMessage() = _state.update { it.copy(message = null) }
}

data class LeaveUiState(
    val canApprove: Boolean = false,
    val statusFilter: String? = null,
    val isSubmitting: Boolean = false,
    val message: UiMessage? = null,
)

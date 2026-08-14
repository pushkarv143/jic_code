package com.greenwood.school.ui.feature.facilities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.isSelfService
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.dto.HostelDto
import com.greenwood.school.data.remote.dto.HostelFeeDto
import com.greenwood.school.data.remote.dto.HostelRoomDto
import com.greenwood.school.data.remote.dto.HostelStudentDto
import com.greenwood.school.data.remote.dto.HostelVisitorDto
import com.greenwood.school.domain.repository.HostelRepository
import com.greenwood.school.ui.common.PagedListState
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.common.UiMessage
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.LoadMoreFooter
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

/** Hostel: residents, rooms, the visitor log and monthly hostel fees. */
@Composable
fun HostelScreen(
    onBack: (() -> Unit)? = null,
    viewModel: HostelViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val residents by viewModel.residents.collectAsStateWithLifecycle()
    val visitors by viewModel.visitors.collectAsStateWithLifecycle()
    val fees by viewModel.fees.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message?.id) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.consumeMessage()
        }
    }

    val tabs = listOf("Residents", "Rooms", "Visitors", "Fees")

    Scaffold(
        topBar = { AppTopBar(title = "Hostel", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = tab == index,
                        onClick = {
                            tab = index
                            when (index) {
                                2 -> viewModel.refreshVisitors()
                                3 -> viewModel.refreshFees()
                            }
                        },
                        text = { Text(label) },
                    )
                }
            }

            when (tab) {
                0 -> ResidentsTab(residents, viewModel)
                1 -> RoomsTab(state.hostels, state.rooms, state.isLoading, viewModel)
                2 -> VisitorsTab(visitors, viewModel)
                else -> FeesTab(fees, state.canManage, viewModel)
            }
        }
    }
}

@Composable
private fun ResidentsTab(residents: PagedListState<HostelStudentDto>, viewModel: HostelViewModel) {
    when {
        residents.isInitialLoad -> FullScreenLoader()
        residents.error != null -> ErrorView(error = residents.error, onRetry = viewModel::refreshResidents)
        residents.isEmpty -> EmptyView(title = "No residents", message = "Allocated students appear here.")
        else -> LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(residents.items.size, key = { residents.items[it].id }) { index ->
                val resident = residents.items[index]
                EntityRowCard(
                    title = resident.studentName ?: "Student #${resident.studentId}",
                    subtitle = listOfNotNull(resident.hostelName, resident.roomNumber?.let { "Room $it" })
                        .joinToString(" · ")
                        .ifBlank { null },
                    metadata = "Allocated ${Formatters.date(resident.allocationDate)}",
                    leadingInitials = Formatters.initials(resident.studentName.orEmpty()),
                    trailing = { StatusChip(resident.status) },
                )
            }
            item {
                LoadMoreFooter(residents.isAppending, residents.appendError, viewModel::loadMoreResidents)
            }
        }
    }
}

@Composable
private fun RoomsTab(
    hostels: List<HostelDto>,
    rooms: Map<Long, List<HostelRoomDto>>,
    isLoading: Boolean,
    viewModel: HostelViewModel,
) {
    when {
        isLoading -> FullScreenLoader()
        hostels.isEmpty() -> EmptyView(title = "No hostels")
        else -> LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            hostels.forEach { hostel ->
                item(key = "hostel-${hostel.id}") {
                    Text(
                        "${hostel.name} · ${Formatters.humanizeEnum(hostel.type)}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )
                }
                val hostelRooms = rooms[hostel.id].orEmpty()
                if (hostelRooms.isEmpty()) {
                    item(key = "empty-${hostel.id}") {
                        Text(
                            "No rooms recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                items(hostelRooms.size, key = { "room-${hostelRooms[it].id}" }) { index ->
                    val room = hostelRooms[index]
                    EntityRowCard(
                        title = "Room ${room.roomNumber}",
                        subtitle = "${room.occupiedCount} of ${room.capacity} beds taken",
                        metadata = if (room.isFull) "Full" else "${room.vacancies} free",
                        leadingInitials = room.roomNumber.takeLast(2),
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitorsTab(visitors: PagedListState<HostelVisitorDto>, viewModel: HostelViewModel) {
    when {
        visitors.isInitialLoad -> FullScreenLoader()
        visitors.error != null -> ErrorView(error = visitors.error, onRetry = viewModel::refreshVisitors)
        visitors.isEmpty -> EmptyView(title = "No visitors logged")
        else -> LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visitors.items.size, key = { visitors.items[it].id }) { index ->
                val visitor = visitors.items[index]
                EntityRowCard(
                    title = visitor.visitorName,
                    subtitle = listOfNotNull(visitor.relation, visitor.studentName)
                        .joinToString(" · ")
                        .ifBlank { null },
                    metadata = "In ${Formatters.time(visitor.checkIn)}" +
                        (visitor.checkOut?.let { " · out ${Formatters.time(it)}" } ?: ""),
                    leadingInitials = Formatters.initials(visitor.visitorName),
                    trailing = {
                        if (visitor.isCheckedIn) {
                            TextButton(onClick = { viewModel.checkout(visitor) }) { Text("Check out") }
                        }
                    },
                )
            }
            item { LoadMoreFooter(visitors.isAppending, visitors.appendError, viewModel::loadMoreVisitors) }
        }
    }
}

@Composable
private fun FeesTab(fees: PagedListState<HostelFeeDto>, canManage: Boolean, viewModel: HostelViewModel) {
    when {
        fees.isInitialLoad -> FullScreenLoader()
        fees.error != null -> ErrorView(error = fees.error, onRetry = viewModel::refreshFees)
        fees.isEmpty -> EmptyView(title = "No hostel fees recorded")
        else -> LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(fees.items.size, key = { fees.items[it].id }) { index ->
                val fee = fees.items[index]
                EntityRowCard(
                    title = fee.studentName ?: "Student #${fee.studentId}",
                    subtitle = Formatters.monthYear(fee.month, fee.year),
                    metadata = Formatters.currency(fee.amount),
                    leadingInitials = Formatters.initials(fee.studentName.orEmpty()),
                    trailing = {
                        Column(horizontalAlignment = Alignment.End) {
                            StatusChip(fee.paidStatus)
                            if (canManage && fee.paidStatus != "PAID") {
                                TextButton(onClick = { viewModel.markPaid(fee) }) { Text("Mark paid") }
                            }
                        }
                    },
                )
            }
            item { LoadMoreFooter(fees.isAppending, fees.appendError, viewModel::loadMoreFees) }
        }
    }
}

@HiltViewModel
class HostelViewModel @Inject constructor(
    private val hostelRepository: HostelRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val user = sessionManager.currentUser
    private val role = Role.from(user?.role)
    private val ownStudentId = user?.studentId.takeIf { role.isSelfService }

    private val _state = MutableStateFlow(HostelUiState(canManage = !role.isSelfService))
    val state: StateFlow<HostelUiState> = _state.asStateFlow()

    private val residentLoader = PagedLoader(viewModelScope) { page, size ->
        hostelRepository.getResidents(studentId = ownStudentId, page = page, size = size)
    }
    val residents = residentLoader.state

    private val visitorLoader = PagedLoader(viewModelScope) { page, size ->
        hostelRepository.getVisitors(studentId = ownStudentId, page = page, size = size)
    }
    val visitors = visitorLoader.state

    private val feeLoader = PagedLoader(viewModelScope) { page, size ->
        hostelRepository.getHostelFees(studentId = ownStudentId, page = page, size = size)
    }
    val fees = feeLoader.state

    init {
        residentLoader.refresh()
        loadRooms()
    }

    /**
     * Rooms hang off hostels, and there is no flat `/hostel-rooms` list endpoint —
     * so fetch the hostels, then their rooms in parallel, and key the result by
     * hostel id for the grouped list.
     */
    private fun loadRooms() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            coroutineScope {
                val hostels = hostelRepository.getHostels().getOrNull().orEmpty()
                val rooms = hostels
                    .map { hostel -> hostel.id to async { hostelRepository.getRooms(hostel.id).getOrNull().orEmpty() } }
                    .associate { (id, deferred) -> id to deferred.await() }
                _state.update { it.copy(isLoading = false, hostels = hostels, rooms = rooms) }
            }
        }
    }

    fun checkout(visitor: HostelVisitorDto) {
        viewModelScope.launch {
            when (val result = hostelRepository.checkoutVisitor(visitor.id)) {
                is ApiResult.Success -> {
                    visitorLoader.replaceWhere({ it.id == visitor.id }, result.data)
                    _state.update { it.copy(message = UiMessage.success("${visitor.visitorName} checked out.")) }
                }

                is ApiResult.Failure -> _state.update { it.copy(message = UiMessage.error(result.error)) }
            }
        }
    }

    fun markPaid(fee: HostelFeeDto) {
        viewModelScope.launch {
            when (val result = hostelRepository.markHostelFeePaid(fee.id)) {
                is ApiResult.Success -> {
                    feeLoader.replaceWhere({ it.id == fee.id }, result.data)
                    _state.update { it.copy(message = UiMessage.success("Marked as paid.")) }
                }

                is ApiResult.Failure -> _state.update { it.copy(message = UiMessage.error(result.error)) }
            }
        }
    }

    fun refreshResidents() = residentLoader.refresh()
    fun loadMoreResidents() = residentLoader.loadMore()
    fun refreshVisitors() = visitorLoader.refresh()
    fun loadMoreVisitors() = visitorLoader.loadMore()
    fun refreshFees() = feeLoader.refresh()
    fun loadMoreFees() = feeLoader.loadMore()
    fun consumeMessage() = _state.update { it.copy(message = null) }
}

data class HostelUiState(
    val canManage: Boolean = false,
    val hostels: List<HostelDto> = emptyList(),
    val rooms: Map<Long, List<HostelRoomDto>> = emptyMap(),
    val isLoading: Boolean = false,
    val message: UiMessage? = null,
)

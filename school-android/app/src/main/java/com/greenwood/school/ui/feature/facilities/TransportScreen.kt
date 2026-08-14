package com.greenwood.school.ui.feature.facilities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.isSelfService
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.dto.BusDto
import com.greenwood.school.data.remote.dto.DriverDto
import com.greenwood.school.data.remote.dto.RouteDto
import com.greenwood.school.domain.repository.TransportRepository
import com.greenwood.school.ui.common.PagedListState
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.LoadMoreFooter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Transport: the fleet (buses, routes, drivers) and per-student assignments.
 *
 * A student or parent gets a single tab showing only their own assignment — the
 * whole fleet is not their business and the backend scopes it the same way.
 */
@Composable
fun TransportScreen(
    onBack: (() -> Unit)? = null,
    viewModel: TransportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    val tabs = remember(state.isSelfService) {
        if (state.isSelfService) listOf("My transport") else listOf("Assignments", "Routes", "Buses", "Drivers")
    }

    Scaffold(topBar = { AppTopBar(title = "Transport", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (tabs.size > 1) {
                ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
                    tabs.forEachIndexed { index, label ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                    }
                }
            }

            when {
                state.isLoading && tab != 0 -> FullScreenLoader()
                state.error != null && tab != 0 -> ErrorView(error = state.error!!, onRetry = viewModel::load)
                tab == 0 -> AssignmentsTab(assignments, viewModel)
                tab == 1 -> RoutesTab(state.routes)
                tab == 2 -> BusesTab(state.buses)
                else -> DriversTab(state.drivers)
            }
        }
    }
}

@Composable
private fun AssignmentsTab(
    assignments: PagedListState<com.greenwood.school.data.remote.dto.StudentTransportDto>,
    viewModel: TransportViewModel,
) {
    when {
        assignments.isInitialLoad -> FullScreenLoader()
        assignments.error != null -> ErrorView(error = assignments.error, onRetry = viewModel::refreshAssignments)
        assignments.isEmpty -> EmptyView(
            title = "No transport assignments",
            message = "Students using school transport appear here.",
        )

        else -> LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(assignments.items.size, key = { assignments.items[it].id }) { index ->
                val assignment = assignments.items[index]
                EntityRowCard(
                    title = assignment.studentName ?: "Student #${assignment.studentId}",
                    subtitle = assignment.routeName,
                    metadata = listOfNotNull(
                        assignment.pickupPointName?.let { "Pickup: $it" },
                        assignment.admissionNumber,
                    ).joinToString(" · ").ifBlank { null },
                    leadingInitials = Formatters.initials(assignment.studentName.orEmpty()),
                    trailing = {
                        Text(
                            Formatters.currency(assignment.monthlyFee),
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        )
                    },
                )
            }
            item {
                LoadMoreFooter(
                    assignments.isAppending,
                    assignments.appendError,
                    viewModel::loadMoreAssignments,
                )
            }
        }
    }
}

@Composable
private fun RoutesTab(routes: List<RouteDto>) {
    if (routes.isEmpty()) {
        EmptyView(title = "No routes")
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(routes.size, key = { routes[it].id }) { index ->
            val route = routes[index]
            EntityRowCard(
                title = route.routeName,
                subtitle = "${route.startPoint.orEmpty()} → ${route.endPoint.orEmpty()}",
                metadata = listOfNotNull(
                    route.busNumber?.let { "Bus $it" },
                    route.pickupPointCount?.let { "$it stops" },
                ).joinToString(" · ").ifBlank { null },
                leadingInitials = Formatters.initials(route.routeName),
            )
        }
    }
}

@Composable
private fun BusesTab(buses: List<BusDto>) {
    if (buses.isEmpty()) {
        EmptyView(title = "No buses")
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(buses.size, key = { buses[it].id }) { index ->
            val bus = buses[index]
            EntityRowCard(
                title = "Bus ${bus.busNumber}",
                subtitle = bus.driverName?.let { "Driver: $it" } ?: "No driver assigned",
                metadata = listOfNotNull(
                    bus.registrationNumber,
                    bus.vehicleModel,
                    "Capacity ${bus.capacity}",
                ).joinToString(" · "),
                leadingInitials = bus.busNumber.takeLast(2),
            )
        }
    }
}

@Composable
private fun DriversTab(drivers: List<DriverDto>) {
    if (drivers.isEmpty()) {
        EmptyView(title = "No drivers")
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(drivers.size, key = { drivers[it].id }) { index ->
            val driver = drivers[index]
            EntityRowCard(
                title = driver.name,
                subtitle = driver.phone,
                metadata = driver.licenseNumber?.let { "Licence $it" },
                leadingInitials = Formatters.initials(driver.name),
            )
        }
    }
}

@HiltViewModel
class TransportViewModel @Inject constructor(
    private val transportRepository: TransportRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val user = sessionManager.currentUser
    private val role = Role.from(user?.role)
    private val ownStudentId = user?.studentId.takeIf { role.isSelfService }

    private val _state = MutableStateFlow(TransportUiState(isSelfService = role.isSelfService))
    val state: StateFlow<TransportUiState> = _state.asStateFlow()

    private val assignmentLoader = PagedLoader(viewModelScope) { page, size ->
        transportRepository.getStudentTransport(studentId = ownStudentId, page = page, size = size)
    }
    val assignments = assignmentLoader.state

    init {
        assignmentLoader.refresh()
        if (!role.isSelfService) load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            coroutineScope {
                val routes = async { transportRepository.getRoutes().getOrNull().orEmpty() }
                val buses = async { transportRepository.getBuses().getOrNull().orEmpty() }
                val drivers = async { transportRepository.getDrivers().getOrNull().orEmpty() }
                _state.update {
                    it.copy(
                        isLoading = false,
                        routes = routes.await(),
                        buses = buses.await(),
                        drivers = drivers.await(),
                    )
                }
            }
        }
    }

    fun refreshAssignments() = assignmentLoader.refresh()
    fun loadMoreAssignments() = assignmentLoader.loadMore()
}

data class TransportUiState(
    val isSelfService: Boolean = false,
    val routes: List<RouteDto> = emptyList(),
    val buses: List<BusDto> = emptyList(),
    val drivers: List<DriverDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
)

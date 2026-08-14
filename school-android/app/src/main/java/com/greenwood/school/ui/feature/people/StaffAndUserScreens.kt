package com.greenwood.school.ui.feature.people

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.data.remote.dto.StaffDto
import com.greenwood.school.data.remote.dto.UserDto
import com.greenwood.school.domain.repository.PeopleRepository
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.common.UiMessage
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.FilterChipRow
import com.greenwood.school.ui.components.PagedListScaffold
import com.greenwood.school.ui.components.StatusChip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ------------------------------------------------------------------------- */
/* Staff directory — read-only, matching the backend's `GET /staff`.          */
/* ------------------------------------------------------------------------- */

@Composable
fun StaffListScreen(
    onBack: (() -> Unit)? = null,
    viewModel: StaffListViewModel = hiltViewModel(),
) {
    val search by viewModel.search.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()

    PagedListScaffold(
        title = "Staff",
        state = list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        searchQuery = search,
        onSearchChange = viewModel::onSearchChange,
        searchPlaceholder = "Search staff",
        emptyTitle = "No staff found",
        key = { it.id },
    ) { staff: StaffDto ->
        EntityRowCard(
            title = staff.displayName,
            subtitle = listOfNotNull(staff.designationName, staff.departmentName).joinToString(" · "),
            metadata = staff.employeeId?.let { "Employee ID $it" },
            leadingInitials = Formatters.initials(staff.displayName),
            trailing = { StatusChip(staff.status) },
        )
    }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class StaffListViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
) : ViewModel() {

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val loader = PagedLoader(viewModelScope) { page, size ->
        peopleRepository.getStaff(search = _search.value, page = page, size = size)
    }
    val list = loader.state

    init {
        loader.refresh()
        viewModelScope.launch {
            // No distinctUntilChanged here: this is a StateFlow, which already
            // conflates equal values, and applying the operator to one is a
            // deprecation error in current coroutines.
            _search.drop(1)
                .debounce(Constants.SEARCH_DEBOUNCE_MS)
                .collect { loader.refresh() }
        }
    }

    fun onSearchChange(value: String) = _search.update { value }
    fun refresh() = loader.refresh()
    fun loadMore() = loader.loadMore()
}

/* ------------------------------------------------------------------------- */
/* User accounts — activate/deactivate, the web `UserListPage`.               */
/* ------------------------------------------------------------------------- */

@Composable
fun UserListScreen(
    onBack: (() -> Unit)? = null,
    viewModel: UserListViewModel = hiltViewModel(),
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    PagedListScaffold(
        title = "Users",
        state = list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        searchQuery = filters.search,
        onSearchChange = viewModel::onSearchChange,
        searchPlaceholder = "Search by name or username",
        hasActiveFilters = filters.role != null,
        message = message,
        onMessageShown = viewModel::consumeMessage,
        emptyTitle = "No users found",
        key = { it.id },
        filterSheet = { dismiss ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Filter by role", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                FilterChipRow(
                    options = Role.entries.filter { it != Role.UNKNOWN }.map { it.wireName },
                    selected = filters.role,
                    onSelected = viewModel::onRoleSelected,
                )
                Row {
                    TextButton(onClick = { viewModel.onRoleSelected(null) }) { Text("Clear") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = dismiss) { Text("Done") }
                }
                Spacer(Modifier.height(16.dp))
            }
        },
    ) { user ->
        EntityRowCard(
            title = user.fullName,
            subtitle = "@${user.username} · ${Role.from(user.role).label}",
            metadata = user.email,
            leadingInitials = user.initials,
            trailing = {
                // A switch rather than a menu: activate/deactivate is the only action
                // this screen has, and it reads as the state it controls.
                Switch(
                    checked = user.active,
                    onCheckedChange = { viewModel.setActive(user, it) },
                )
            },
        )
    }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class UserListViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(UserFilters())
    val filters: StateFlow<UserFilters> = _filters.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val loader = PagedLoader(viewModelScope) { page, size ->
        peopleRepository.getUsers(
            search = _filters.value.search,
            role = _filters.value.role,
            page = page,
            size = size,
        )
    }
    val list = loader.state

    init {
        loader.refresh()
        viewModelScope.launch {
            _filters.map { it.search }.distinctUntilChanged().drop(1)
                .debounce(Constants.SEARCH_DEBOUNCE_MS)
                .collect { loader.refresh() }
        }
    }

    fun onSearchChange(value: String) = _filters.update { it.copy(search = value) }

    fun onRoleSelected(role: String?) {
        _filters.update { it.copy(role = role) }
        loader.refresh()
    }

    fun setActive(user: UserDto, active: Boolean) {
        viewModelScope.launch {
            when (val result = peopleRepository.setUserActive(user.id, active)) {
                is ApiResult.Success -> {
                    // Patch the row in place so the switch settles immediately instead
                    // of waiting on a full page reload.
                    loader.replaceWhere({ it.id == user.id }, result.data)
                    _message.value = UiMessage.success(
                        if (active) "${user.fullName} activated." else "${user.fullName} deactivated.",
                    )
                }

                is ApiResult.Failure -> _message.value = UiMessage.error(result.error)
            }
        }
    }

    fun consumeMessage() { _message.value = null }
    fun refresh() = loader.refresh()
    fun loadMore() = loader.loadMore()
}

data class UserFilters(val search: String = "", val role: String? = null)

/* ------------------------------------------------------------------------- */
/* Parent portal — "My Children".                                             */
/* ------------------------------------------------------------------------- */

@Composable
fun MyChildrenScreen(
    onOpenStudent: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: MyChildrenViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    com.greenwood.school.ui.components.SimpleListScaffold(
        title = "My Children",
        items = state.children,
        isLoading = state.isLoading,
        error = state.error,
        onRefresh = viewModel::load,
        onBack = onBack,
        emptyTitle = "No children linked",
        emptyMessage = "Ask the school office to link your children to this account.",
        key = { it.studentId },
    ) { child ->
        EntityRowCard(
            title = child.displayName,
            subtitle = child.classSection,
            metadata = child.admissionNumber,
            imageUrl = child.photoUrl,
            leadingInitials = Formatters.initials(child.displayName),
            showChevron = true,
            onClick = { onOpenStudent(child.studentId) },
        )
    }
}

@HiltViewModel
class MyChildrenViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MyChildrenUiState())
    val state: StateFlow<MyChildrenUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            _state.value = when (val result = peopleRepository.getMyChildren()) {
                is ApiResult.Success -> MyChildrenUiState(children = result.data, isLoading = false)
                is ApiResult.Failure -> MyChildrenUiState(isLoading = false, error = result.error)
            }
        }
    }
}

data class MyChildrenUiState(
    val children: List<com.greenwood.school.data.remote.dto.ParentChildDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: com.greenwood.school.core.network.AppError? = null,
)

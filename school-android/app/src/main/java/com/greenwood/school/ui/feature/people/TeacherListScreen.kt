package com.greenwood.school.ui.feature.people

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.data.remote.dto.DepartmentDto
import com.greenwood.school.data.remote.dto.DesignationDto
import com.greenwood.school.data.remote.dto.TeacherDto
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.domain.repository.PeopleRepository
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.components.DropdownField
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

/** Teacher directory — the mobile form of `TeacherListPage`. */
@Composable
fun TeacherListScreen(
    onOpenTeacher: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    /** Null hides the add button for roles the API would reject anyway. */
    onAddTeacher: (() -> Unit)? = null,
    viewModel: TeacherListViewModel = hiltViewModel(),
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()

    PagedListScaffold(
        title = "Teachers",
        state = list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        searchQuery = filters.search,
        onSearchChange = viewModel::onSearchChange,
        searchPlaceholder = "Search by name or employee ID",
        hasActiveFilters = filters.hasActiveFilters,
        emptyTitle = "No teachers found",
        emptyMessage = "Try a different search or clear the filters.",
        onAdd = onAddTeacher,
        addContentDescription = "Add teacher",
        key = { it.id },
        filterSheet = { dismiss ->
            TeacherFilterSheet(
                filters = filters,
                onDepartment = viewModel::onDepartmentSelected,
                onDesignation = viewModel::onDesignationSelected,
                onStatus = viewModel::onStatusSelected,
                onDone = dismiss,
            )
        },
    ) { teacher ->
        EntityRowCard(
            title = teacher.displayName,
            subtitle = listOfNotNull(teacher.designationName, teacher.departmentName)
                .joinToString(" · ")
                .ifBlank { teacher.employeeId },
            metadata = teacher.employeeId?.let { "Employee ID $it" },
            leadingInitials = Formatters.initials(teacher.displayName),
            trailing = { StatusChip(teacher.status) },
            onClick = { onOpenTeacher(teacher.id) },
        )
    }
}

@Composable
private fun TeacherFilterSheet(
    filters: TeacherFilters,
    onDepartment: (DepartmentDto?) -> Unit,
    onDesignation: (DesignationDto?) -> Unit,
    onStatus: (String?) -> Unit,
    onDone: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Filter teachers", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(14.dp))

        DropdownField(
            label = "Department",
            options = filters.departments,
            selected = filters.department,
            onSelected = onDepartment,
            optionLabel = { it.name },
        )
        Spacer(Modifier.height(10.dp))
        DropdownField(
            label = "Designation",
            options = filters.designations,
            selected = filters.designation,
            onSelected = onDesignation,
            optionLabel = { it.name },
        )
        Spacer(Modifier.height(14.dp))

        Text("Status", style = MaterialTheme.typography.labelLarge)
        FilterChipRow(options = Constants.STAFF_STATUSES, selected = filters.status, onSelected = onStatus)

        Spacer(Modifier.height(8.dp))
        Row {
            TextButton(
                onClick = {
                    onDepartment(null)
                    onDesignation(null)
                    onStatus(null)
                },
            ) { Text("Clear all") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("Done") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class TeacherListViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(TeacherFilters())
    val filters: StateFlow<TeacherFilters> = _filters.asStateFlow()

    private val loader = PagedLoader(viewModelScope) { page, size ->
        val current = _filters.value
        peopleRepository.getTeachers(
            search = current.search,
            departmentId = current.department?.id,
            designationId = current.designation?.id,
            status = current.status,
            page = page,
            size = size,
        )
    }

    val list = loader.state

    init {
        loader.refresh()
        viewModelScope.launch {
            _filters.update {
                it.copy(
                    departments = academicRepository.getDepartments().getOrNull().orEmpty(),
                    designations = academicRepository.getDesignations().getOrNull().orEmpty(),
                )
            }
        }
        viewModelScope.launch {
            _filters.map { it.search }.distinctUntilChanged().drop(1)
                .debounce(Constants.SEARCH_DEBOUNCE_MS)
                .collect { loader.refresh() }
        }
    }

    fun onSearchChange(value: String) = _filters.update { it.copy(search = value) }

    fun onDepartmentSelected(value: DepartmentDto?) {
        _filters.update { it.copy(department = value) }
        loader.refresh()
    }

    fun onDesignationSelected(value: DesignationDto?) {
        _filters.update { it.copy(designation = value) }
        loader.refresh()
    }

    fun onStatusSelected(value: String?) {
        _filters.update { it.copy(status = value) }
        loader.refresh()
    }

    fun refresh() = loader.refresh()
    fun loadMore() = loader.loadMore()
}

data class TeacherFilters(
    val search: String = "",
    val departments: List<DepartmentDto> = emptyList(),
    val designations: List<DesignationDto> = emptyList(),
    val department: DepartmentDto? = null,
    val designation: DesignationDto? = null,
    val status: String? = null,
) {
    val hasActiveFilters: Boolean get() = department != null || designation != null || status != null
}

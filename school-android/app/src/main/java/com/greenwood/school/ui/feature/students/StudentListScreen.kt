package com.greenwood.school.ui.feature.students

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.data.remote.dto.StudentDto
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.DropdownField
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FilterChipRow
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.PagedLazyColumn
import com.greenwood.school.ui.components.ResultCount
import com.greenwood.school.ui.components.SearchField
import com.greenwood.school.ui.components.StatusChip

/**
 * The web app shows students in an eleven-column DataGrid. On a phone that becomes
 * a list of cards carrying only what identifies a student — name, admission number,
 * class/section, status — with everything else one tap away on the detail screen.
 *
 * Filters move into a bottom sheet so they don't eat vertical space that the list needs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    onOpenStudent: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    onAddStudent: (() -> Unit)? = null,
    viewModel: StudentListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Students",
                subtitle = state.list.totalElements.takeIf { it > 0 }
                    ?.let { "${Formatters.number(it)} records" },
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            Icons.Outlined.FilterList,
                            contentDescription = "Filters",
                            tint = if (state.hasActiveFilters) {
                                androidx.compose.material3.MaterialTheme.colorScheme.primary
                            } else {
                                androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            // Only the roles the backend lets write students get the FAB; the rest
            // would just be shown a 403 after filling in the whole form.
            if (onAddStudent != null) {
                FloatingActionButton(onClick = onAddStudent) {
                    Icon(Icons.Outlined.Add, contentDescription = "Admit student")
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                value = state.search,
                onValueChange = viewModel::onSearchChange,
                placeholder = "Search by name or admission no.",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )

            when {
                state.list.isInitialLoad -> FullScreenLoader()

                state.list.error != null -> ErrorView(
                    error = state.list.error!!,
                    onRetry = viewModel::refresh,
                )

                state.list.isEmpty -> EmptyView(
                    title = if (state.search.isNotBlank() || state.hasActiveFilters) {
                        "No matching students"
                    } else {
                        "No students yet"
                    },
                    message = if (state.search.isNotBlank() || state.hasActiveFilters) {
                        "Try a different search or clear the filters."
                    } else {
                        "Students appear here once they've been admitted."
                    },
                )

                else -> PagedLazyColumn(
                    state = state.list,
                    onLoadMore = viewModel::loadMore,
                    key = { it.id },
                    header = { ResultCount(state.list.items.size, state.list.totalElements) },
                ) { student ->
                    StudentRow(student = student, onClick = { onOpenStudent(student.id) })
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }, sheetState = sheetState) {
            FilterSheet(
                state = state,
                onClassSelected = viewModel::onClassSelected,
                onSectionSelected = viewModel::onSectionSelected,
                onStatusSelected = viewModel::onStatusSelected,
                onDone = { showFilters = false },
            )
        }
    }
}

@Composable
private fun StudentRow(student: StudentDto, onClick: () -> Unit) {
    EntityRowCard(
        title = student.displayName,
        subtitle = "${student.admissionNumber} · ${student.classSection}",
        metadata = student.rollNumber?.let { "Roll no. $it" },
        imageUrl = student.photoUrl,
        leadingInitials = Formatters.initials(student.displayName),
        trailing = { StatusChip(student.status) },
        onClick = onClick,
    )
}

@Composable
private fun FilterSheet(
    state: StudentListUiState,
    onClassSelected: (com.greenwood.school.data.remote.dto.SchoolClassDto?) -> Unit,
    onSectionSelected: (com.greenwood.school.data.remote.dto.SectionDto?) -> Unit,
    onStatusSelected: (String?) -> Unit,
    onDone: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Filter students", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(14.dp))

        DropdownField(
            label = "Class",
            options = state.classes,
            selected = state.selectedClass,
            onSelected = onClassSelected,
            optionLabel = { it.className },
        )
        Spacer(Modifier.height(10.dp))

        DropdownField(
            label = "Section",
            options = state.sections,
            selected = state.selectedSection,
            onSelected = onSectionSelected,
            // Sections belong to a class, so this stays locked until one is chosen.
            enabled = state.selectedClass != null,
            optionLabel = { it.sectionName },
        )
        Spacer(Modifier.height(14.dp))

        Text("Status", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        FilterChipRow(
            options = Constants.STUDENT_STATUSES,
            selected = state.status,
            onSelected = onStatusSelected,
        )

        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.Row {
            TextButton(
                onClick = {
                    onClassSelected(null)
                    onStatusSelected(null)
                },
            ) { Text("Clear all") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("Done") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

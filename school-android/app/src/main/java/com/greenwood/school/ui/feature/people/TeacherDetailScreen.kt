package com.greenwood.school.ui.feature.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.data.remote.dto.TeacherDto
import com.greenwood.school.domain.repository.PeopleRepository
import com.greenwood.school.navigation.Routes
import com.greenwood.school.ui.common.UiState
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.Avatar
import com.greenwood.school.ui.components.DetailRow
import com.greenwood.school.ui.components.SectionCard
import com.greenwood.school.ui.components.StateHost
import com.greenwood.school.ui.components.StatusChip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Teacher profile — `GET /teachers/{id}` returns everything in one call. */
@Composable
fun TeacherDetailScreen(
    onBack: () -> Unit,
    /** Null hides the action for roles the API would reject anyway. */
    onEdit: (() -> Unit)? = null,
    viewModel: TeacherDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Coming back from the form, re-read the record so the edits are on screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = (state as? UiState.Success)?.data?.displayName ?: "Teacher",
                onBack = onBack,
                actions = {
                    if (onEdit != null && state is UiState.Success) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit teacher")
                        }
                    }
                },
            )
        },
    ) { padding ->
        StateHost(
            state = state,
            modifier = Modifier.padding(padding),
            onRetry = viewModel::load,
            emptyTitle = "Teacher not found",
        ) { teacher -> TeacherDetailContent(teacher) }
    }
}

@Composable
private fun TeacherDetailContent(teacher: TeacherDto) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(initials = Formatters.initials(teacher.displayName), size = 84.dp)
            Spacer(Modifier.height(10.dp))
            Text(teacher.displayName, style = MaterialTheme.typography.titleLarge)
            Text(
                listOfNotNull(teacher.designationName, teacher.departmentName).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            StatusChip(teacher.status)
        }

        SectionCard(title = "Employment") {
            DetailRow("Employee ID", teacher.employeeId)
            DetailRow("Department", teacher.departmentName)
            DetailRow("Designation", teacher.designationName)
            DetailRow("Type", Formatters.humanizeEnum(teacher.employmentType))
            DetailRow("Joined", Formatters.date(teacher.joiningDate))
            DetailRow("Experience", teacher.experienceYears?.let { "$it years" })
            DetailRow("Qualification", teacher.qualification)
        }

        SectionCard(title = "Personal") {
            DetailRow("Date of birth", Formatters.date(teacher.dateOfBirth))
            DetailRow("Gender", Formatters.humanizeEnum(teacher.gender))
            DetailRow("Blood group", teacher.bloodGroup)
            DetailRow("Emergency contact", teacher.emergencyContact)
        }

        SectionCard(title = "Contact") {
            DetailRow("Username", teacher.username)
            DetailRow("Email", teacher.email)
            DetailRow("Phone", teacher.phone)
            DetailRow("Address", teacher.address)
            DetailRow("City", teacher.city)
            DetailRow("State", teacher.state)
            DetailRow("PIN code", teacher.pincode)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@HiltViewModel
class TeacherDetailViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val teacherId: Long = checkNotNull(savedStateHandle[Routes.ARG_TEACHER_ID])

    private val _state = MutableStateFlow<UiState<TeacherDto>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherDto>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = peopleRepository.getTeacher(teacherId)) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.Failure -> UiState.Error(result.error)
            }
        }
    }
}

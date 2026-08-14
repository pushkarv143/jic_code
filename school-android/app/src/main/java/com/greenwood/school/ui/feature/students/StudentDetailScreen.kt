package com.greenwood.school.ui.feature.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.data.remote.dto.StudentDto
import com.greenwood.school.domain.repository.StudentRepository
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

/**
 * Full student profile.
 *
 * `GET /students/{id}` already returns guardians, medical details and documents
 * nested, so the whole screen is one request — the web app's four tabs become four
 * stacked sections and the user scrolls instead of tapping between them.
 */
@Composable
fun StudentDetailScreen(
    studentId: Long,
    onBack: () -> Unit,
    viewModel: StudentDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = (state as? UiState.Success)?.data?.displayName ?: "Student",
                onBack = onBack,
            )
        },
    ) { padding ->
        StateHost(
            state = state,
            modifier = Modifier.padding(padding),
            onRetry = viewModel::load,
            emptyTitle = "Student not found",
        ) { student ->
            StudentDetailContent(student)
        }
    }
}

@Composable
private fun StudentDetailContent(student: StudentDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(
                initials = Formatters.initials(student.displayName),
                imageUrl = student.photoUrl,
                size = 84.dp,
            )
            Spacer(Modifier.height(10.dp))
            Text(student.displayName, style = MaterialTheme.typography.titleLarge)
            Text(
                "${student.admissionNumber} · ${student.classSection}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            StatusChip(student.status)
        }

        SectionCard(title = "Academic") {
            DetailRow("Class", student.className)
            DetailRow("Section", student.sectionName)
            DetailRow("Roll number", student.rollNumber)
            DetailRow("Admission number", student.admissionNumber)
            DetailRow("Admission date", Formatters.date(student.admissionDate))
        }

        SectionCard(title = "Personal") {
            DetailRow("Date of birth", Formatters.date(student.dateOfBirth))
            DetailRow("Gender", Formatters.humanizeEnum(student.gender))
            DetailRow("Blood group", student.bloodGroup)
            DetailRow("Religion", student.religion)
            DetailRow("Category", student.category)
        }

        SectionCard(title = "Contact") {
            DetailRow("Email", student.email)
            DetailRow("Phone", student.phone)
            DetailRow("Address", student.address)
            DetailRow("City", student.city)
            DetailRow("State", student.state)
            DetailRow("PIN code", student.pincode)
        }

        val guardians = student.guardians.orEmpty()
        if (guardians.isNotEmpty()) {
            SectionCard(title = "Guardians") {
                guardians.forEach { guardian ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(
                            text = guardian.name + if (guardian.isPrimary) "  (primary)" else "",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        DetailRow("Relation", guardian.relation)
                        DetailRow("Phone", guardian.phone)
                        DetailRow("Email", guardian.email)
                        DetailRow("Occupation", guardian.occupation)
                    }
                }
            }
        }

        student.medicalDetails?.let { medical ->
            SectionCard(title = "Medical") {
                DetailRow("Height", medical.heightCm?.let { "$it cm" })
                DetailRow("Weight", medical.weightKg?.let { "$it kg" })
                DetailRow("Allergies", medical.allergies)
                DetailRow("Conditions", medical.medicalConditions)
                DetailRow("Doctor", medical.doctorName)
                DetailRow("Doctor contact", medical.doctorContact)
            }
        }

        val documents = student.documents.orEmpty()
        if (documents.isNotEmpty()) {
            SectionCard(title = "Documents") {
                documents.forEach { document ->
                    DetailRow(
                        Formatters.humanizeEnum(document.documentType),
                        Formatters.date(document.uploadedAt),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val studentId: Long = checkNotNull(savedStateHandle[Routes.ARG_STUDENT_ID]) {
        "StudentDetailScreen requires a ${Routes.ARG_STUDENT_ID} argument"
    }

    private val _state = MutableStateFlow<UiState<StudentDto>>(UiState.Loading)
    val state: StateFlow<UiState<StudentDto>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = studentRepository.getStudent(studentId)) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.Failure -> UiState.Error(result.error)
            }
        }
    }
}

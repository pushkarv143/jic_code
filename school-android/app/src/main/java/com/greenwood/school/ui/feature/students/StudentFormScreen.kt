package com.greenwood.school.ui.feature.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Validators
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.data.remote.dto.AcademicYearDto
import com.greenwood.school.data.remote.dto.GuardianRequestDto
import com.greenwood.school.data.remote.dto.SchoolClassDto
import com.greenwood.school.data.remote.dto.SectionDto
import com.greenwood.school.data.remote.dto.StudentRequestDto
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.domain.repository.StudentRepository
import com.greenwood.school.navigation.Routes
import com.greenwood.school.ui.common.UiMessage
import com.greenwood.school.ui.components.AppTextField
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.DateField
import com.greenwood.school.ui.components.DropdownField
import com.greenwood.school.ui.components.EnumDropdownField
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Admit or edit a student.
 *
 * The web form is a wide multi-column layout; here it is a single scrolling column
 * grouped into the same four sections. On create, one primary guardian is captured
 * inline (the backend accepts nested guardians on `POST /students` and creates
 * them in the same transaction); on edit, guardians are managed through their own
 * endpoints from the detail screen instead.
 */
@Composable
fun StudentFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: StudentFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message?.id) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            AppTopBar(title = if (state.isEditing) "Edit student" else "Admit student", onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            FullScreenLoader()
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionCard(title = "Personal") {
                AppTextField(
                    value = state.firstName,
                    onValueChange = { viewModel.update { copy(firstName = it, firstNameError = null) } },
                    label = "First name",
                    required = true,
                    error = state.firstNameError,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.lastName,
                    onValueChange = { viewModel.update { copy(lastName = it, lastNameError = null) } },
                    label = "Last name",
                    required = true,
                    error = state.lastNameError,
                )
                Spacer(Modifier.height(10.dp))
                DateField(
                    label = "Date of birth",
                    value = state.dateOfBirth,
                    onValueChange = { viewModel.update { copy(dateOfBirth = it, dateOfBirthError = null) } },
                    required = true,
                    error = state.dateOfBirthError,
                )
                Spacer(Modifier.height(10.dp))
                EnumDropdownField(
                    label = "Gender",
                    options = Constants.GENDERS,
                    selected = state.gender,
                    onSelected = { viewModel.update { copy(gender = it) } },
                    required = true,
                )
                Spacer(Modifier.height(10.dp))
                DropdownField(
                    label = "Blood group",
                    options = Constants.BLOOD_GROUPS,
                    selected = state.bloodGroup,
                    onSelected = { viewModel.update { copy(bloodGroup = it) } },
                    optionLabel = { it },
                )
            }

            SectionCard(title = "Academic") {
                DropdownField(
                    label = "Class",
                    options = state.classes,
                    selected = state.selectedClass,
                    onSelected = viewModel::onClassSelected,
                    optionLabel = { it.className },
                    required = true,
                    error = state.classError,
                )
                Spacer(Modifier.height(10.dp))
                DropdownField(
                    label = "Section",
                    options = state.sections,
                    selected = state.selectedSection,
                    onSelected = { viewModel.update { copy(selectedSection = it, sectionError = null) } },
                    optionLabel = { it.sectionName },
                    enabled = state.selectedClass != null,
                    required = true,
                    error = state.sectionError,
                )
                Spacer(Modifier.height(10.dp))
                DropdownField(
                    label = "Academic year",
                    options = state.years,
                    selected = state.selectedYear,
                    onSelected = { viewModel.update { copy(selectedYear = it, yearError = null) } },
                    optionLabel = { it.yearName },
                    required = true,
                    error = state.yearError,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.rollNumber,
                    onValueChange = { viewModel.update { copy(rollNumber = it, rollNumberError = null) } },
                    label = "Roll number",
                    required = true,
                    error = state.rollNumberError,
                )
                Spacer(Modifier.height(10.dp))
                DateField(
                    label = "Admission date",
                    value = state.admissionDate,
                    onValueChange = { viewModel.update { copy(admissionDate = it) } },
                    required = true,
                )
            }

            SectionCard(title = "Contact") {
                AppTextField(
                    value = state.email,
                    onValueChange = { viewModel.update { copy(email = it, emailError = null) } },
                    label = "Email",
                    error = state.emailError,
                    keyboardType = KeyboardType.Email,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.phone,
                    onValueChange = { viewModel.update { copy(phone = it, phoneError = null) } },
                    label = "Phone",
                    error = state.phoneError,
                    keyboardType = KeyboardType.Phone,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.address,
                    onValueChange = { viewModel.update { copy(address = it) } },
                    label = "Address",
                    singleLine = false,
                    minLines = 2,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.city,
                    onValueChange = { viewModel.update { copy(city = it) } },
                    label = "City",
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.state,
                    onValueChange = { viewModel.update { copy(state = it) } },
                    label = "State",
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.pincode,
                    onValueChange = { viewModel.update { copy(pincode = it, pincodeError = null) } },
                    label = "PIN code",
                    error = state.pincodeError,
                    keyboardType = KeyboardType.Number,
                )
            }

            if (!state.isEditing) {
                SectionCard(title = "Primary guardian") {
                    AppTextField(
                        value = state.guardianName,
                        onValueChange = { viewModel.update { copy(guardianName = it, guardianNameError = null) } },
                        label = "Name",
                        required = true,
                        error = state.guardianNameError,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.guardianRelation,
                        onValueChange = { viewModel.update { copy(guardianRelation = it) } },
                        label = "Relation",
                        required = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.guardianPhone,
                        onValueChange = { viewModel.update { copy(guardianPhone = it, guardianPhoneError = null) } },
                        label = "Phone",
                        required = true,
                        error = state.guardianPhoneError,
                        keyboardType = KeyboardType.Phone,
                    )
                }
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text(if (state.isSaving) "Saving…" else if (state.isEditing) "Save changes" else "Admit student") }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@HiltViewModel
class StudentFormViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val academicRepository: AcademicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** The route passes -1 for "new"; anything else is the id being edited. */
    private val studentId: Long? = savedStateHandle.get<Long>(Routes.ARG_STUDENT_ID)?.takeIf { it > 0 }

    private val _state = MutableStateFlow(StudentFormUiState(isEditing = studentId != null))
    val state: StateFlow<StudentFormUiState> = _state.asStateFlow()

    fun update(transform: StudentFormUiState.() -> StudentFormUiState) = _state.update(transform)

    fun consumeMessage() = _state.update { it.copy(message = null) }

    init {
        viewModelScope.launch {
            val classes = academicRepository.getClasses().getOrNull().orEmpty()
            val years = academicRepository.getAcademicYears().getOrNull().orEmpty()
            _state.update {
                it.copy(
                    classes = classes,
                    years = years,
                    selectedYear = years.firstOrNull { year -> year.isCurrent } ?: years.firstOrNull(),
                )
            }
            studentId?.let { loadStudent(it) } ?: _state.update { it.copy(isLoading = false) }
        }
    }

    fun onClassSelected(schoolClass: SchoolClassDto?) {
        _state.update {
            it.copy(selectedClass = schoolClass, selectedSection = null, sections = emptyList(), classError = null)
        }
        schoolClass?.let { loadSections(it.id) }
    }

    private fun loadSections(classId: Long, preselectSectionId: Long? = null) {
        viewModelScope.launch {
            val sections = academicRepository.getSections(classId).getOrNull().orEmpty()
            _state.update {
                it.copy(
                    sections = sections,
                    selectedSection = preselectSectionId?.let { id -> sections.firstOrNull { s -> s.id == id } }
                        ?: it.selectedSection,
                )
            }
        }
    }

    private suspend fun loadStudent(id: Long) {
        when (val result = studentRepository.getStudent(id)) {
            is ApiResult.Success -> {
                val student = result.data
                _state.update {
                    it.copy(
                        isLoading = false,
                        firstName = student.firstName.orEmpty(),
                        lastName = student.lastName.orEmpty(),
                        email = student.email.orEmpty(),
                        phone = student.phone.orEmpty(),
                        rollNumber = student.rollNumber.orEmpty(),
                        dateOfBirth = Formatters.parseDate(student.dateOfBirth),
                        admissionDate = Formatters.parseDate(student.admissionDate) ?: LocalDate.now(),
                        gender = student.gender ?: Constants.GENDERS.first(),
                        bloodGroup = student.bloodGroup,
                        address = student.address.orEmpty(),
                        city = student.city.orEmpty(),
                        state = student.state.orEmpty(),
                        pincode = student.pincode.orEmpty(),
                        selectedClass = it.classes.firstOrNull { c -> c.id == student.classId },
                        selectedYear = it.years.firstOrNull { y -> y.id == student.academicYearId }
                            ?: it.selectedYear,
                    )
                }
                loadSections(student.classId, preselectSectionId = student.sectionId)
            }

            is ApiResult.Failure -> _state.update {
                it.copy(isLoading = false, error = result.error, message = UiMessage.error(result.error))
            }
        }
    }

    fun save() {
        val current = _state.value
        if (current.isSaving) return

        val validated = current.copy(
            firstNameError = Validators.required(current.firstName, "First name"),
            lastNameError = Validators.required(current.lastName, "Last name"),
            rollNumberError = Validators.required(current.rollNumber, "Roll number"),
            dateOfBirthError = if (current.dateOfBirth == null) "Date of birth is required" else null,
            emailError = Validators.email(current.email, required = false),
            phoneError = Validators.phone(current.phone, required = false),
            pincodeError = Validators.pincode(current.pincode),
            classError = if (current.selectedClass == null) "Select a class" else null,
            sectionError = if (current.selectedSection == null) "Select a section" else null,
            yearError = if (current.selectedYear == null) "Select an academic year" else null,
            guardianNameError = if (!current.isEditing) {
                Validators.required(current.guardianName, "Guardian name")
            } else {
                null
            },
            guardianPhoneError = if (!current.isEditing) {
                Validators.phone(current.guardianPhone)
            } else {
                null
            },
        )
        _state.value = validated
        if (validated.hasErrors) {
            _state.update { it.copy(message = UiMessage("Please fix the highlighted fields.", isError = true)) }
            return
        }

        val request = StudentRequestDto(
            firstName = current.firstName.trim(),
            lastName = current.lastName.trim(),
            email = current.email.trim().takeIf { it.isNotBlank() },
            phone = current.phone.trim().takeIf { it.isNotBlank() },
            classId = current.selectedClass!!.id,
            sectionId = current.selectedSection!!.id,
            academicYearId = current.selectedYear!!.id,
            rollNumber = current.rollNumber.trim(),
            admissionDate = Formatters.apiDate(current.admissionDate),
            dateOfBirth = Formatters.apiDate(current.dateOfBirth!!),
            gender = current.gender,
            bloodGroup = current.bloodGroup,
            address = current.address.trim().takeIf { it.isNotBlank() },
            city = current.city.trim().takeIf { it.isNotBlank() },
            state = current.state.trim().takeIf { it.isNotBlank() },
            pincode = current.pincode.trim().takeIf { it.isNotBlank() },
            guardians = if (current.isEditing) {
                null
            } else {
                listOf(
                    GuardianRequestDto(
                        name = current.guardianName.trim(),
                        relation = current.guardianRelation.trim().ifBlank { "GUARDIAN" },
                        phone = current.guardianPhone.trim(),
                        isPrimary = true,
                    ),
                )
            },
        )

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (studentId == null) {
                studentRepository.createStudent(request)
            } else {
                studentRepository.updateStudent(studentId, request)
            }
            when (result) {
                is ApiResult.Success -> _state.update {
                    it.copy(isSaving = false, isSaved = true, message = UiMessage.success("Student saved."))
                }

                is ApiResult.Failure -> _state.update {
                    // Surface the backend's field-level messages against the same fields.
                    val fields = (result.error as? AppError.Validation)?.fieldErrors.orEmpty()
                    it.copy(
                        isSaving = false,
                        message = UiMessage.error(result.error),
                        rollNumberError = fields["rollNumber"] ?: it.rollNumberError,
                        emailError = fields["email"] ?: it.emailError,
                        phoneError = fields["phone"] ?: it.phoneError,
                    )
                }
            }
        }
    }
}

data class StudentFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,

    val classes: List<SchoolClassDto> = emptyList(),
    val sections: List<SectionDto> = emptyList(),
    val years: List<AcademicYearDto> = emptyList(),
    val selectedClass: SchoolClassDto? = null,
    val selectedSection: SectionDto? = null,
    val selectedYear: AcademicYearDto? = null,

    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val rollNumber: String = "",
    val dateOfBirth: LocalDate? = null,
    val admissionDate: LocalDate = LocalDate.now(),
    val gender: String = Constants.GENDERS.first(),
    val bloodGroup: String? = null,
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",

    val guardianName: String = "",
    val guardianRelation: String = "",
    val guardianPhone: String = "",

    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val rollNumberError: String? = null,
    val dateOfBirthError: String? = null,
    val pincodeError: String? = null,
    val classError: String? = null,
    val sectionError: String? = null,
    val yearError: String? = null,
    val guardianNameError: String? = null,
    val guardianPhoneError: String? = null,

    val error: AppError? = null,
    val message: UiMessage? = null,
) {
    val hasErrors: Boolean
        get() = listOf(
            firstNameError, lastNameError, emailError, phoneError, rollNumberError,
            dateOfBirthError, pincodeError, classError, sectionError, yearError,
            guardianNameError, guardianPhoneError,
        ).any { it != null }
}

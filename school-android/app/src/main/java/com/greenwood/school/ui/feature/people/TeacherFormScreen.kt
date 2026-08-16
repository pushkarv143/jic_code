package com.greenwood.school.ui.feature.people

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
import com.greenwood.school.data.remote.dto.DepartmentDto
import com.greenwood.school.data.remote.dto.DesignationDto
import com.greenwood.school.data.remote.dto.TeacherRequestDto
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.domain.repository.PeopleRepository
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
 * Add or edit a teacher — the mobile counterpart of the web's `TeacherFormPage`.
 *
 * Creating a teacher creates their login account in the same transaction, so the
 * username and password are captured here and only here: `POST /teachers` takes
 * them, `PUT /teachers/{id}` has no such fields and a password is changed through
 * the profile screen instead. That is why the account section disappears on edit.
 */
@Composable
fun TeacherFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: TeacherFormViewModel = hiltViewModel(),
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
            AppTopBar(title = if (state.isEditing) "Edit teacher" else "Add teacher", onBack = onBack)
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
            if (!state.isEditing) {
                SectionCard(title = "Login account") {
                    AppTextField(
                        value = state.username,
                        onValueChange = { viewModel.update { copy(username = it, usernameError = null) } },
                        label = "Username",
                        required = true,
                        error = state.usernameError,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.password,
                        onValueChange = { viewModel.update { copy(password = it, passwordError = null) } },
                        label = "Password",
                        required = true,
                        error = state.passwordError,
                    )
                }
            }

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
                    onValueChange = { viewModel.update { copy(lastName = it) } },
                    label = "Last name",
                )
                Spacer(Modifier.height(10.dp))
                DateField(
                    label = "Date of birth",
                    value = state.dateOfBirth,
                    onValueChange = { viewModel.update { copy(dateOfBirth = it) } },
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

            SectionCard(title = "Employment") {
                DropdownField(
                    label = "Department",
                    options = state.departments,
                    selected = state.selectedDepartment,
                    onSelected = { viewModel.update { copy(selectedDepartment = it, departmentError = null) } },
                    optionLabel = { it.name },
                    required = true,
                    error = state.departmentError,
                )
                Spacer(Modifier.height(10.dp))
                DropdownField(
                    label = "Designation",
                    options = state.designations,
                    selected = state.selectedDesignation,
                    onSelected = { viewModel.update { copy(selectedDesignation = it, designationError = null) } },
                    optionLabel = { it.name },
                    required = true,
                    error = state.designationError,
                )
                Spacer(Modifier.height(10.dp))
                EnumDropdownField(
                    label = "Employment type",
                    options = Constants.EMPLOYMENT_TYPES,
                    selected = state.employmentType,
                    onSelected = { viewModel.update { copy(employmentType = it) } },
                    required = true,
                )
                Spacer(Modifier.height(10.dp))
                DateField(
                    label = "Joining date",
                    value = state.joiningDate,
                    onValueChange = { viewModel.update { copy(joiningDate = it) } },
                    required = true,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.qualification,
                    onValueChange = { viewModel.update { copy(qualification = it) } },
                    label = "Qualification",
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.experienceYears,
                    onValueChange = { viewModel.update { copy(experienceYears = it, experienceYearsError = null) } },
                    label = "Experience (years)",
                    error = state.experienceYearsError,
                    keyboardType = KeyboardType.Number,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.salary,
                    onValueChange = { viewModel.update { copy(salary = it, salaryError = null) } },
                    label = "Salary",
                    error = state.salaryError,
                    keyboardType = KeyboardType.Number,
                )
            }

            SectionCard(title = "Contact") {
                AppTextField(
                    value = state.email,
                    onValueChange = { viewModel.update { copy(email = it, emailError = null) } },
                    label = "Email",
                    required = true,
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
                    value = state.emergencyContact,
                    onValueChange = {
                        viewModel.update { copy(emergencyContact = it, emergencyContactError = null) }
                    },
                    label = "Emergency contact",
                    error = state.emergencyContactError,
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

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text(if (state.isSaving) "Saving…" else if (state.isEditing) "Save changes" else "Add teacher") }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@HiltViewModel
class TeacherFormViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val academicRepository: AcademicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** The route passes -1 for "new"; anything else is the id being edited. */
    private val teacherId: Long? = savedStateHandle.get<Long>(Routes.ARG_TEACHER_ID)?.takeIf { it > 0 }

    private val _state = MutableStateFlow(TeacherFormUiState(isEditing = teacherId != null))
    val state: StateFlow<TeacherFormUiState> = _state.asStateFlow()

    fun update(transform: TeacherFormUiState.() -> TeacherFormUiState) = _state.update(transform)

    fun consumeMessage() = _state.update { it.copy(message = null) }

    init {
        viewModelScope.launch {
            val departments = academicRepository.getDepartments().getOrNull().orEmpty()
            val designations = academicRepository.getDesignations().getOrNull().orEmpty()
            _state.update { it.copy(departments = departments, designations = designations) }
            teacherId?.let { loadTeacher(it) } ?: _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadTeacher(id: Long) {
        when (val result = peopleRepository.getTeacher(id)) {
            is ApiResult.Success -> {
                val teacher = result.data
                _state.update {
                    it.copy(
                        isLoading = false,
                        // Carried so the update payload keeps the account it belongs to;
                        // the field itself is not editable after creation.
                        username = teacher.username.orEmpty(),
                        firstName = teacher.firstName.orEmpty(),
                        lastName = teacher.lastName.orEmpty(),
                        email = teacher.email.orEmpty(),
                        phone = teacher.phone.orEmpty(),
                        gender = teacher.gender ?: Constants.GENDERS.first(),
                        bloodGroup = teacher.bloodGroup,
                        dateOfBirth = Formatters.parseDate(teacher.dateOfBirth),
                        joiningDate = Formatters.parseDate(teacher.joiningDate) ?: LocalDate.now(),
                        qualification = teacher.qualification.orEmpty(),
                        experienceYears = teacher.experienceYears?.toString().orEmpty(),
                        // Plain digits, not the grouped display format — this value goes
                        // straight back into an editable field.
                        salary = teacher.salary
                            ?.let { value -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString() }
                            .orEmpty(),
                        employmentType = teacher.employmentType ?: Constants.EMPLOYMENT_TYPES.first(),
                        emergencyContact = teacher.emergencyContact.orEmpty(),
                        address = teacher.address.orEmpty(),
                        city = teacher.city.orEmpty(),
                        state = teacher.state.orEmpty(),
                        pincode = teacher.pincode.orEmpty(),
                        selectedDepartment = it.departments.firstOrNull { d -> d.id == teacher.departmentId },
                        selectedDesignation = it.designations.firstOrNull { d -> d.id == teacher.designationId },
                    )
                }
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
            usernameError = if (current.isEditing) null else Validators.required(current.username, "Username"),
            passwordError = if (current.isEditing) null else Validators.password(current.password),
            firstNameError = Validators.required(current.firstName, "First name"),
            emailError = Validators.email(current.email),
            // Optional server-side — the pattern accepts an empty value — so only
            // check what was actually typed.
            phoneError = Validators.phone(current.phone, required = false),
            emergencyContactError = Validators.phone(current.emergencyContact, required = false),
            pincodeError = Validators.pincode(current.pincode),
            departmentError = if (current.selectedDepartment == null) "Select a department" else null,
            designationError = if (current.selectedDesignation == null) "Select a designation" else null,
            experienceYearsError = current.experienceYears.takeIf { it.isNotBlank() }
                ?.let { Validators.positiveNumber(it, "Experience", allowZero = true) },
            salaryError = current.salary.takeIf { it.isNotBlank() }
                ?.let { Validators.positiveNumber(it, "Salary", allowZero = true) },
        )
        _state.value = validated
        if (validated.hasErrors) {
            _state.update { it.copy(message = UiMessage("Please fix the highlighted fields.", isError = true)) }
            return
        }

        val request = TeacherRequestDto(
            username = current.username.trim(),
            email = current.email.trim(),
            // Only sent on create: PUT /teachers/{id} has no password field, and
            // changing one goes through the profile screen.
            password = current.password.takeIf { !current.isEditing && it.isNotBlank() },
            firstName = current.firstName.trim(),
            lastName = current.lastName.trim(),
            phone = current.phone.trim(),
            gender = current.gender,
            departmentId = current.selectedDepartment!!.id,
            designationId = current.selectedDesignation!!.id,
            qualification = current.qualification.trim().takeIf { it.isNotBlank() },
            experienceYears = current.experienceYears.trim().toIntOrNull(),
            joiningDate = Formatters.apiDate(current.joiningDate),
            dateOfBirth = current.dateOfBirth?.let { Formatters.apiDate(it) },
            address = current.address.trim().takeIf { it.isNotBlank() },
            city = current.city.trim().takeIf { it.isNotBlank() },
            state = current.state.trim().takeIf { it.isNotBlank() },
            pincode = current.pincode.trim().takeIf { it.isNotBlank() },
            bloodGroup = current.bloodGroup,
            emergencyContact = current.emergencyContact.trim().takeIf { it.isNotBlank() },
            salary = current.salary.trim().toDoubleOrNull(),
            employmentType = current.employmentType,
        )

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (teacherId == null) {
                peopleRepository.createTeacher(request)
            } else {
                peopleRepository.updateTeacher(teacherId, request)
            }
            when (result) {
                is ApiResult.Success -> _state.update {
                    it.copy(isSaving = false, isSaved = true, message = UiMessage.success("Teacher saved."))
                }

                is ApiResult.Failure -> _state.update {
                    // Surface the backend's field-level messages against the same fields.
                    val fields = (result.error as? AppError.Validation)?.fieldErrors.orEmpty()
                    it.copy(
                        isSaving = false,
                        message = UiMessage.error(result.error),
                        usernameError = fields["username"] ?: it.usernameError,
                        passwordError = fields["password"] ?: it.passwordError,
                        emailError = fields["email"] ?: it.emailError,
                        phoneError = fields["phone"] ?: it.phoneError,
                        emergencyContactError = fields["emergencyContact"] ?: it.emergencyContactError,
                    )
                }
            }
        }
    }
}

data class TeacherFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,

    val departments: List<DepartmentDto> = emptyList(),
    val designations: List<DesignationDto> = emptyList(),
    val selectedDepartment: DepartmentDto? = null,
    val selectedDesignation: DesignationDto? = null,

    val username: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val gender: String = Constants.GENDERS.first(),
    val bloodGroup: String? = null,
    val dateOfBirth: LocalDate? = null,
    val joiningDate: LocalDate = LocalDate.now(),
    val qualification: String = "",
    val experienceYears: String = "",
    val salary: String = "",
    val employmentType: String = Constants.EMPLOYMENT_TYPES.first(),
    val emergencyContact: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",

    val usernameError: String? = null,
    val passwordError: String? = null,
    val firstNameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val emergencyContactError: String? = null,
    val pincodeError: String? = null,
    val departmentError: String? = null,
    val designationError: String? = null,
    val experienceYearsError: String? = null,
    val salaryError: String? = null,

    val error: AppError? = null,
    val message: UiMessage? = null,
) {
    val hasErrors: Boolean
        get() = listOf(
            usernameError, passwordError, firstNameError, emailError, phoneError,
            emergencyContactError, pincodeError, departmentError, designationError,
            experienceYearsError, salaryError,
        ).any { it != null }
}

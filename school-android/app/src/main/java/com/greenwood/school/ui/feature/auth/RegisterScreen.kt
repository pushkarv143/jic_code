package com.greenwood.school.ui.feature.auth

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.Validators
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.domain.repository.AuthRepository
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.AppTextField
import com.greenwood.school.ui.components.DropdownField
import com.greenwood.school.ui.components.PasswordField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Self-registration for STUDENT / PARENT accounts — the only two roles
 * `AuthController.register` accepts. The account lands in a pending state and the
 * school activates it, which the success message says explicitly so nobody waits
 * for a login that will not work yet.
 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
            if (state.isComplete) onBack()
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Create an account", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                "Your account has to be approved by the school before you can sign in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

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
                required = true,
                error = state.phoneError,
                keyboardType = KeyboardType.Phone,
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.username,
                onValueChange = { viewModel.update { copy(username = it, usernameError = null) } },
                label = "Username",
                required = true,
                error = state.usernameError,
            )
            Spacer(Modifier.height(10.dp))
            DropdownField(
                label = "I am a",
                options = listOf(Role.STUDENT, Role.PARENT),
                selected = state.role,
                onSelected = { viewModel.update { copy(role = it) } },
                optionLabel = { it.label },
                required = true,
            )
            Spacer(Modifier.height(10.dp))
            PasswordField(
                value = state.password,
                onValueChange = { viewModel.update { copy(password = it, passwordError = null) } },
                label = "Password",
                error = state.passwordError,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(10.dp))
            PasswordField(
                value = state.confirmPassword,
                onValueChange = { viewModel.update { copy(confirmPassword = it, confirmPasswordError = null) } },
                label = "Confirm password",
                error = state.confirmPasswordError,
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::submit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Create account")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    /** Small helper so the screen can express edits as `copy(...)` without ceremony. */
    fun update(transform: RegisterUiState.() -> RegisterUiState) = _state.update(transform)

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return

        val validated = current.copy(
            firstNameError = Validators.required(current.firstName, "First name"),
            lastNameError = Validators.required(current.lastName, "Last name"),
            emailError = Validators.email(current.email),
            phoneError = Validators.phone(current.phone),
            usernameError = Validators.required(current.username, "Username"),
            passwordError = Validators.password(current.password),
            confirmPasswordError = Validators.confirmPassword(current.password, current.confirmPassword),
        )
        _state.value = validated
        if (validated.hasErrors) return

        _state.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val result = authRepository.register(
                firstName = current.firstName,
                lastName = current.lastName,
                email = current.email,
                phone = current.phone,
                username = current.username,
                password = current.password,
                role = current.role.wireName,
            )
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        isSubmitting = false,
                        isComplete = true,
                        message = "Registration submitted. You can sign in once the school approves your account.",
                    )

                    is ApiResult.Failure -> it.copy(
                        isSubmitting = false,
                        message = result.error.userMessage,
                        // Surface any per-field messages the backend's validator returned.
                        emailError = result.error.fieldError("email") ?: it.emailError,
                        usernameError = result.error.fieldError("username") ?: it.usernameError,
                        passwordError = result.error.fieldError("password") ?: it.passwordError,
                    )
                }
            }
        }
    }
}

/** Pulls one field's message out of a 400/422 `validationErrors` map. */
private fun com.greenwood.school.core.network.AppError.fieldError(field: String): String? =
    (this as? com.greenwood.school.core.network.AppError.Validation)?.fieldErrors?.get(field)

data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val role: Role = Role.STUDENT,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false,
    val message: String? = null,
) {
    val hasErrors: Boolean
        get() = listOf(
            firstNameError, lastNameError, emailError, phoneError,
            usernameError, passwordError, confirmPasswordError,
        ).any { it != null }
}

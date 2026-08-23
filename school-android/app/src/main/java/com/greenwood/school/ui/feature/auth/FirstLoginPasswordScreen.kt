package com.greenwood.school.ui.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

/**
 * The password change a school-provisioned account completes before anything else.
 *
 * <p>Reached automatically: the login response carries {@code mustChangePassword},
 * and the nav graph opens this instead of the dashboard. The server enforces the
 * same rule independently — every other endpoint answers 403 until the password is
 * replaced — so this screen is the convenient route to a rule that holds whether or
 * not the app cooperates. An older build that does not know about it would simply
 * show a dashboard where nothing loaded.
 *
 * <p>It ends at the sign-in screen, not the dashboard. Changing the password revokes
 * every refresh token the account has, so the session running this screen is over
 * the moment the change succeeds; signing out and saying why beats letting the next
 * request fail on a token that has just been invalidated.
 */
@Composable
fun FirstLoginPasswordScreen(
    onPasswordChanged: () -> Unit,
    viewModel: FirstLoginPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isDone) {
        // Signalled once, and the nav graph clears this screen off the back stack.
        onPasswordChanged()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Choose your password", style = MaterialTheme.typography.headlineSmall)
        Text(
            "This account is still using the password the school sent you. Pick one only " +
                "you know — the emailed password stops working straight away, and you will " +
                "sign in again with the new one.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = state.currentPassword,
            onValueChange = viewModel::onCurrentPassword,
            label = { Text("Password from your email (optional)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            isError = state.currentPasswordError != null,
            supportingText = {
                Text(
                    state.currentPasswordError
                        ?: "Leave this empty if you signed in with a code and never " +
                        "received the emailed password.",
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.newPassword,
            onValueChange = viewModel::onNewPassword,
            label = { Text("New password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            isError = state.newPasswordError != null,
            supportingText = {
                Text(
                    state.newPasswordError
                        ?: "At least 8 characters, with an uppercase letter, a lowercase " +
                        "letter, a number and a symbol.",
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmPassword,
            label = { Text("Confirm new password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            isError = state.confirmPasswordError != null,
            supportingText = state.confirmPasswordError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )

        state.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = viewModel::submit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
            }
            Text(if (state.isSubmitting) "Saving…" else "Set password and sign in again")
        }
    }
}

@HiltViewModel
class FirstLoginPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FirstLoginPasswordUiState())
    val state: StateFlow<FirstLoginPasswordUiState> = _state.asStateFlow()

    fun onCurrentPassword(value: String) =
        _state.update { it.copy(currentPassword = value, currentPasswordError = null, formError = null) }

    fun onNewPassword(value: String) =
        _state.update { it.copy(newPassword = value, newPasswordError = null, formError = null) }

    fun onConfirmPassword(value: String) =
        _state.update { it.copy(confirmPassword = value, confirmPasswordError = null, formError = null) }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return

        // The same rule the API enforces, checked here so a typo costs no round trip.
        // Not a substitute for the server check — the server is the one that counts.
        // No error for an empty current password: the server accepts it omitted while
        // the account is still on a generated one, which is the whole point — a code
        // sign-in never saw that password.
        val currentError: String? = null
        val newError = when {
            current.newPassword.isBlank() -> "Choose a new password"
            !POLICY.matches(current.newPassword) ->
                "At least 8 characters, with an uppercase letter, a lowercase letter, " +
                    "a number and a symbol"
            current.currentPassword.isNotBlank() &&
                current.newPassword == current.currentPassword ->
                "Choose a password different from the one you were sent"
            else -> null
        }
        val confirmError = "The passwords do not match"
            .takeIf { current.newPassword != current.confirmPassword }

        if (currentError != null || newError != null || confirmError != null) {
            _state.update {
                it.copy(
                    currentPasswordError = currentError,
                    newPasswordError = newError,
                    confirmPasswordError = confirmError,
                )
            }
            return
        }

        _state.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            val result = authRepository.changePassword(
                current.currentPassword,
                current.newPassword,
                current.confirmPassword,
            )
            when (result) {
                is ApiResult.Success -> {
                    // Signing out is what the server has already done to the tokens.
                    authRepository.logout()
                    _state.update { it.copy(isSubmitting = false, isDone = true) }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, formError = result.error.userMessage)
                }
            }
        }
    }

    private companion object {
        /** Mirrors ChangePasswordRequest's @Pattern on the backend. */
        val POLICY = Regex(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)" +
                "(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$",
        )
    }
}

data class FirstLoginPasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val formError: String? = null,
    val isSubmitting: Boolean = false,
    val isDone: Boolean = false,
)

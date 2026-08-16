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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Validators
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.data.remote.dto.OtpPurpose
import com.greenwood.school.domain.repository.AuthRepository
import com.greenwood.school.ui.components.AppTextField
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.PasswordField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Password recovery by one-time passcode, in three steps on one screen:
 *
 *  1. give the email or phone on the account (`POST /auth/otp/request`)
 *  2. type the six digits that arrive     (`POST /auth/otp/verify`)
 *  3. choose a new password               (`POST /auth/reset-password`)
 *
 * It replaces a flow that mailed a reset *link*. The link pointed at the web
 * frontend, which an Android user has no way to open usefully, so recovering a
 * password on a phone did not really work. A code can be retyped from any mail
 * client, which is what makes this the mobile-appropriate shape.
 *
 * One screen rather than three, because the user leaves for their mail app and
 * comes back — re-navigating would lose what they had typed.
 */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
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

    // Drives the "Resend in Ns" label. Runs only while there is time left, so it
    // stops on its own rather than ticking for the life of the screen.
    LaunchedEffect(state.resendInSeconds) {
        if (state.resendInSeconds > 0) {
            delay(1_000)
            viewModel.tickResendCountdown()
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Reset your password", onBack = onBack) },
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
                "Enter the email address or mobile number on your account. We'll send you a 6-digit code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            AppTextField(
                value = state.destination,
                onValueChange = viewModel::onDestinationChange,
                label = "Email or mobile number",
                required = true,
                error = state.destinationError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                // Locked once a code is out, so the code and the address cannot
                // drift apart — the server checks them together.
                enabled = !state.codeSent,
            )

            Spacer(Modifier.height(16.dp))

            if (!state.codeSent) {
                Button(
                    onClick = viewModel::sendCode,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Send code")
                    }
                }
            }

            if (state.codeSent && !state.codeVerified) {
                Text("Step 2 — enter the code", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "It expires in 5 minutes. Check your spam folder if it hasn't arrived.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                AppTextField(
                    value = state.code,
                    onValueChange = viewModel::onCodeChange,
                    label = "6-digit code",
                    required = true,
                    error = state.codeError,
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::verifyCode,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Verify code")
                    }
                }

                TextButton(
                    onClick = viewModel::sendCode,
                    // The server enforces its own cooldown; matching it here turns a
                    // guaranteed 429 into a label that says how long is left.
                    enabled = state.resendInSeconds == 0 && !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.resendInSeconds > 0) {
                            "Resend in ${state.resendInSeconds}s"
                        } else {
                            "Resend code"
                        },
                    )
                }
            }

            if (state.codeVerified) {
                Text("Step 3 — set a new password", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                PasswordField(
                    value = state.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    label = "New password",
                    error = state.newPasswordError,
                )
                Spacer(Modifier.height(10.dp))
                PasswordField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = "Confirm new password",
                    error = state.confirmPasswordError,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::resetPassword,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Set new password")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onDestinationChange(value: String) =
        _state.update { it.copy(destination = value, destinationError = null) }

    /** Digits only, capped at six — the field cannot hold anything the API would reject. */
    fun onCodeChange(value: String) = _state.update {
        it.copy(code = value.filter(Char::isDigit).take(CODE_LENGTH), codeError = null)
    }

    fun onNewPasswordChange(value: String) =
        _state.update { it.copy(newPassword = value, newPasswordError = null) }

    fun onConfirmPasswordChange(value: String) =
        _state.update { it.copy(confirmPassword = value, confirmPasswordError = null) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun tickResendCountdown() =
        _state.update { it.copy(resendInSeconds = (it.resendInSeconds - 1).coerceAtLeast(0)) }

    fun sendCode() {
        val current = _state.value
        if (current.isSubmitting) return

        val error = validateDestination(current.destination)
        if (error != null) {
            _state.update { it.copy(destinationError = error) }
            return
        }

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = authRepository.requestOtp(current.destination.trim(), OtpPurpose.PASSWORD_RESET)
            _state.update {
                when (result) {
                    // The server answers identically for a registered address and an
                    // unknown one, so the wording must not imply the account was found.
                    is ApiResult.Success -> it.copy(
                        isSubmitting = false,
                        codeSent = true,
                        code = "",
                        resendInSeconds = result.data.resendAfterSeconds,
                        message = "If an account matches, a code has been sent.",
                    )

                    is ApiResult.Failure -> it.copy(
                        isSubmitting = false,
                        message = result.error.userMessage,
                    )
                }
            }
        }
    }

    fun verifyCode() {
        val current = _state.value
        if (current.isSubmitting) return

        if (current.code.length != CODE_LENGTH) {
            _state.update { it.copy(codeError = "Enter the 6-digit code") }
            return
        }

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = authRepository.verifyOtp(
                destination = current.destination.trim(),
                purpose = OtpPurpose.PASSWORD_RESET,
                code = current.code,
            )
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        isSubmitting = false,
                        codeVerified = true,
                        // Held only long enough to complete step 3.
                        resetToken = result.data.resetToken.orEmpty(),
                        resendInSeconds = 0,
                    )

                    is ApiResult.Failure -> it.copy(
                        isSubmitting = false,
                        // Shown against the field rather than as a snackbar: the code is
                        // what was wrong, and it is what they need to retype.
                        codeError = result.error.userMessage,
                    )
                }
            }
        }
    }

    fun resetPassword() {
        val current = _state.value
        if (current.isSubmitting) return

        val passwordError = Validators.password(current.newPassword)
        val confirmError = Validators.confirmPassword(current.newPassword, current.confirmPassword)
        if (passwordError != null || confirmError != null) {
            _state.update { it.copy(newPasswordError = passwordError, confirmPasswordError = confirmError) }
            return
        }

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = authRepository.resetPassword(current.resetToken, current.newPassword)
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        isSubmitting = false,
                        isComplete = true,
                        resetToken = "",
                        message = "Password reset. Sign in with your new password.",
                    )

                    is ApiResult.Failure -> it.copy(isSubmitting = false, message = result.error.userMessage)
                }
            }
        }
    }

    /**
     * The field takes an email or a phone number, so which check applies depends on
     * what was typed. Phone is accepted here because the API supports it; it will
     * be refused with a clear message until an SMS gateway exists.
     */
    private fun validateDestination(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return "Enter your email address"
        return if (trimmed.contains("@")) {
            Validators.email(trimmed)
        } else {
            Validators.phone(trimmed)
        }
    }

    private companion object {
        const val CODE_LENGTH = 6
    }
}

data class ForgotPasswordUiState(
    val destination: String = "",
    val code: String = "",
    val resetToken: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",

    val destinationError: String? = null,
    val codeError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,

    val codeSent: Boolean = false,
    val codeVerified: Boolean = false,
    val resendInSeconds: Int = 0,
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false,
    val message: String? = null,
)

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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Signing in with a emailed code instead of a password.
 *
 * Offered alongside password sign-in rather than replacing it, for the parents and
 * students who mostly cannot remember a password they set once at admission.
 *
 * It is not a weaker door than the one already there: anyone who can read the code
 * could equally have used "forgot password" to take the account. Both are gated on
 * the same mailbox — this is a shorter path to the same place, not a lower bar.
 */
@Composable
fun OtpLoginScreen(
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: OtpLoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

    LaunchedEffect(state.resendInSeconds) {
        if (state.resendInSeconds > 0) {
            delay(1_000)
            viewModel.tickResendCountdown()
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Sign in with a code", onBack = onBack) },
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
                "We'll email a 6-digit code to the address on your account. No password needed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            AppTextField(
                value = state.destination,
                onValueChange = viewModel::onDestinationChange,
                label = "Email",
                required = true,
                error = state.destinationError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                // Locked once a code is out: the server checks the code against the
                // address it was sent to, so they must not drift apart.
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
            } else {
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
                    onClick = viewModel::signIn,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Sign in")
                    }
                }
                TextButton(
                    onClick = viewModel::sendCode,
                    enabled = state.resendInSeconds == 0 && !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.resendInSeconds > 0) "Resend in ${state.resendInSeconds}s" else "Resend code",
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@HiltViewModel
class OtpLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OtpLoginUiState())
    val state: StateFlow<OtpLoginUiState> = _state.asStateFlow()

    fun onDestinationChange(value: String) =
        _state.update { it.copy(destination = value, destinationError = null) }

    fun onCodeChange(value: String) = _state.update {
        it.copy(code = value.filter(Char::isDigit).take(CODE_LENGTH), codeError = null)
    }

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
            val result = authRepository.requestOtp(current.destination.trim(), OtpPurpose.LOGIN)
            _state.update {
                when (result) {
                    // Same wording as password recovery, and for the same reason: the
                    // server will not say whether the address is registered.
                    is ApiResult.Success -> it.copy(
                        isSubmitting = false,
                        codeSent = true,
                        code = "",
                        resendInSeconds = result.data.resendAfterSeconds,
                        message = "If an account matches, a code has been sent.",
                    )

                    is ApiResult.Failure -> it.copy(isSubmitting = false, message = result.error.userMessage)
                }
            }
        }
    }

    fun signIn() {
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
                purpose = OtpPurpose.LOGIN,
                code = current.code,
            )
            _state.update {
                when (result) {
                    // The repository has already stored the session by this point, so
                    // there is nothing to carry — only somewhere to go.
                    is ApiResult.Success -> it.copy(isSubmitting = false, isSignedIn = true)

                    is ApiResult.Failure -> it.copy(
                        isSubmitting = false,
                        codeError = result.error.userMessage,
                    )
                }
            }
        }
    }

    private fun validateDestination(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return "Enter your email address"
        return if (trimmed.contains("@")) Validators.email(trimmed) else Validators.phone(trimmed)
    }

    private companion object {
        const val CODE_LENGTH = 6
    }
}

data class OtpLoginUiState(
    val destination: String = "",
    val code: String = "",
    val destinationError: String? = null,
    val codeError: String? = null,
    val codeSent: Boolean = false,
    val resendInSeconds: Int = 0,
    val isSubmitting: Boolean = false,
    val isSignedIn: Boolean = false,
    val message: String? = null,
)

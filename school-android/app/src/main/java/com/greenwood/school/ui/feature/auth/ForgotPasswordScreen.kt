package com.greenwood.school.ui.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Validators
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.domain.repository.AuthRepository
import com.greenwood.school.ui.components.AppTextField
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.PasswordField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Password recovery, in the same two steps as the web app:
 *
 *  1. request a reset link by email (`POST /auth/forgot-password`)
 *  2. paste the token from that email and set a new password
 *     (`POST /auth/reset-password`)
 *
 * Both live on one screen because on mobile the user is switching to their mail app
 * and back; making them re-navigate would lose the entered email.
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

    Scaffold(
        topBar = { AppTopBar(title = "Reset your password", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp),
        ) {
            Text(
                "Enter the email on your account and we'll send a reset link.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            AppTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                required = true,
                error = state.emailError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                enabled = !state.linkSent,
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = viewModel::sendLink,
                enabled = !state.isSubmitting && !state.linkSent,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (state.isSubmitting && !state.linkSent) {
                    CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send reset link")
                }
            }

            if (state.linkSent) {
                Spacer(Modifier.height(28.dp))
                Text("Step 2 — set a new password", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Paste the token from the email we just sent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                AppTextField(
                    value = state.token,
                    onValueChange = viewModel::onTokenChange,
                    label = "Reset token",
                    required = true,
                    error = state.tokenError,
                )
                Spacer(Modifier.height(10.dp))
                PasswordField(
                    value = state.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    label = "New password",
                    error = state.newPasswordError,
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
        }
    }
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null) }
    fun onTokenChange(value: String) = _state.update { it.copy(token = value, tokenError = null) }
    fun onNewPasswordChange(value: String) = _state.update { it.copy(newPassword = value, newPasswordError = null) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun sendLink() {
        val current = _state.value
        val emailError = Validators.email(current.email)
        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = authRepository.forgotPassword(current.email)
            _state.update {
                when (result) {
                    // The backend deliberately answers the same way whether or not the
                    // address exists, so we must not imply the account was found.
                    is ApiResult.Success -> it.copy(
                        isSubmitting = false,
                        linkSent = true,
                        message = "If an account exists for that email, a reset link has been sent.",
                    )

                    is ApiResult.Failure -> it.copy(isSubmitting = false, message = result.error.userMessage)
                }
            }
        }
    }

    fun resetPassword() {
        val current = _state.value
        val tokenError = Validators.required(current.token, "Reset token")
        val passwordError = Validators.password(current.newPassword)
        if (tokenError != null || passwordError != null) {
            _state.update { it.copy(tokenError = tokenError, newPasswordError = passwordError) }
            return
        }

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = authRepository.resetPassword(current.token.trim(), current.newPassword)
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        isSubmitting = false,
                        isComplete = true,
                        message = "Password reset. Sign in with your new password.",
                    )

                    is ApiResult.Failure -> it.copy(isSubmitting = false, message = result.error.userMessage)
                }
            }
        }
    }
}

data class ForgotPasswordUiState(
    val email: String = "",
    val token: String = "",
    val newPassword: String = "",
    val emailError: String? = null,
    val tokenError: String? = null,
    val newPasswordError: String? = null,
    val linkSent: Boolean = false,
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false,
    val message: String? = null,
)

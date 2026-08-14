package com.greenwood.school.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) = _state.update {
        it.copy(username = value, usernameError = null, formError = null)
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(password = value, passwordError = null, formError = null)
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return

        // Login validates presence only — the backend owns the credential policy, and
        // running the full password rules here would reject valid legacy passwords.
        val usernameError = "Username is required".takeIf { current.username.isBlank() }
        val passwordError = "Password is required".takeIf { current.password.isBlank() }
        if (usernameError != null || passwordError != null) {
            _state.update { it.copy(usernameError = usernameError, passwordError = passwordError) }
            return
        }

        _state.update { it.copy(isSubmitting = true, formError = null) }

        viewModelScope.launch {
            when (val result = authRepository.login(current.username, current.password)) {
                is ApiResult.Success ->
                    // Navigation is driven by the session flow in MainViewModel, so there
                    // is nothing to do here but stop the spinner.
                    _state.update { it.copy(isSubmitting = false, isSignedIn = true) }

                is ApiResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, formError = result.error)
                }
            }
        }
    }

    fun consumeError() = _state.update { it.copy(formError = null) }
}

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val isSignedIn: Boolean = false,
    /**
     * Kept as an [AppError] rather than a string so the screen can treat a 403
     * "your account is not active" differently from a network failure.
     */
    val formError: AppError? = null,
)

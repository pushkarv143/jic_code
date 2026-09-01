package com.greenwood.school.ui.feature.profile

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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.Validators
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.dto.UserDto
import com.greenwood.school.domain.repository.AuthRepository
import com.greenwood.school.ui.components.Avatar
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.ConfirmDialog
import com.greenwood.school.ui.components.DetailRow
import com.greenwood.school.ui.components.PasswordField
import com.greenwood.school.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.greenwood.school.ui.theme.BrandAmber
import com.greenwood.school.ui.theme.BrandIndigo
import com.greenwood.school.ui.theme.BrandIndigoDark
import com.greenwood.school.ui.theme.BrandIndigoLight

/**
 * "My Profile" — the read-only account card plus the two actions the web app's
 * profile menu offers: change password and sign out.
 *
 * The profile itself is not editable here because the backend has no
 * `PUT /auth/me`; personal details are maintained by the school through the
 * student/teacher records.
 */
@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmSignOut by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(state.isSignedOut) {
        if (state.isSignedOut) onSignedOut()
    }

    Scaffold(
        topBar = { AppTopBar(title = "My Profile", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val user = state.user

            // Gradient hero banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(BrandIndigoDark, BrandIndigo, BrandIndigoLight.copy(alpha = 0.85f))
                        )
                    )
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar with white ring
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Avatar(
                            initials = user?.initials ?: "?",
                            imageUrl = user?.profileImage,
                            size = 80.dp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        user?.fullName.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    // Role pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                    ) {
                        Text(
                            Role.from(user?.role).label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandAmber,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            SectionCard(title = "Account") {
                DetailRow("Username", user?.username)
                DetailRow("Email", user?.email)
                DetailRow("Phone", user?.phone)
                DetailRow("Gender", Formatters.humanizeEnum(user?.gender))
                DetailRow("Status", if (user?.active == true) "Active" else "Inactive")
                DetailRow("Last sign-in", Formatters.dateTime(user?.lastLogin))
                DetailRow("Member since", Formatters.date(user?.createdAt))
            }

            SectionCard(title = "Change password") {
                PasswordField(
                    value = state.currentPassword,
                    onValueChange = viewModel::onCurrentPasswordChange,
                    label = "Current password",
                    error = state.currentPasswordError,
                )
                Spacer(Modifier.height(10.dp))
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
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = viewModel::changePassword,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Update password") }
            }

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = { confirmSignOut = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Sign out")
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmSignOut) {
        ConfirmDialog(
            title = "Sign out?",
            message = "You'll need to sign in again to use the app.",
            confirmLabel = "Sign out",
            onConfirm = {
                confirmSignOut = false
                viewModel.signOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState(user = sessionManager.currentUser))
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.session.collect { session ->
                _state.update { it.copy(user = session?.user) }
            }
        }
        // Pull a fresh copy so a role change made by an admin shows up here.
        viewModelScope.launch { authRepository.refreshProfile() }
    }

    fun onCurrentPasswordChange(v: String) = _state.update { it.copy(currentPassword = v, currentPasswordError = null) }
    fun onNewPasswordChange(v: String) = _state.update { it.copy(newPassword = v, newPasswordError = null) }
    fun onConfirmPasswordChange(v: String) = _state.update { it.copy(confirmPassword = v, confirmPasswordError = null) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun changePassword() {
        val current = _state.value
        if (current.isSubmitting) return

        val currentError = Validators.required(current.currentPassword, "Current password")
        val newError = Validators.password(current.newPassword)
        val confirmError = Validators.confirmPassword(current.newPassword, current.confirmPassword)
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

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = authRepository.changePassword(
                current.currentPassword,
                current.newPassword,
                current.confirmPassword,
            )
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        isSubmitting = false,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                        message = "Password updated.",
                    )

                    is ApiResult.Failure -> it.copy(isSubmitting = false, message = result.error.userMessage)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // Repository clears local state regardless of the network result, so the
            // user is always signed out on this device.
            authRepository.logout()
            _state.update { it.copy(isSignedOut = true) }
        }
    }
}

data class ProfileUiState(
    val user: UserDto? = null,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isSubmitting: Boolean = false,
    val isSignedOut: Boolean = false,
    val message: String? = null,
)

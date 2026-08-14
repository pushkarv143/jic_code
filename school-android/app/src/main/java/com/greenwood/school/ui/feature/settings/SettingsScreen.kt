package com.greenwood.school.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.BuildConfig
import com.greenwood.school.core.common.Validators
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.data.remote.dto.SchoolInfoDto
import com.greenwood.school.data.remote.dto.SystemSettingDto
import com.greenwood.school.domain.repository.SettingsRepository
import com.greenwood.school.ui.common.UiMessage
import com.greenwood.school.ui.components.AppTextField
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.DetailRow
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * School info (editable) and system settings (read-only here).
 *
 * System settings are shown but not edited on mobile: they are free-form key/value
 * pairs whose semantics live in the backend, and a mistyped value in a phone form
 * would be a poor way to discover that. The web admin screen remains the place to
 * change them.
 */
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message?.id) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Settings", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> FullScreenLoader()
            state.error != null -> ErrorView(error = state.error!!, onRetry = viewModel::load)
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionCard(title = "School information") {
                    AppTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = "Name",
                        required = true,
                        error = state.nameError,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.address,
                        onValueChange = viewModel::onAddressChange,
                        label = "Address",
                        singleLine = false,
                        minLines = 2,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = "Phone",
                        error = state.phoneError,
                        keyboardType = KeyboardType.Phone,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        label = "Email",
                        error = state.emailError,
                        keyboardType = KeyboardType.Email,
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = viewModel::save,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                    ) { Text(if (state.isSaving) "Saving…" else "Save school information") }
                }

                if (state.systemSettings.isNotEmpty()) {
                    SectionCard(title = "System settings") {
                        state.systemSettings.forEach { setting ->
                            DetailRow(setting.key, setting.value)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Edit these from the web admin console.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SectionCard(title = "About this app") {
                    DetailRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    DetailRow("Environment", BuildConfig.FLAVOR)
                    DetailRow("API", BuildConfig.BASE_URL)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            coroutineScope {
                val info = async { settingsRepository.getSchoolInfo() }
                val system = async { settingsRepository.getSystemSettings().getOrNull().orEmpty() }

                when (val result = info.await()) {
                    is ApiResult.Success -> _state.value = SettingsUiState(
                        isLoading = false,
                        schoolInfo = result.data,
                        name = result.data.name,
                        address = result.data.address,
                        phone = result.data.phone,
                        email = result.data.email,
                        systemSettings = system.await(),
                    )

                    is ApiResult.Failure ->
                        _state.value = SettingsUiState(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v, nameError = null) }
    fun onAddressChange(v: String) = _state.update { it.copy(address = v) }
    fun onPhoneChange(v: String) = _state.update { it.copy(phone = v, phoneError = null) }
    fun onEmailChange(v: String) = _state.update { it.copy(email = v, emailError = null) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun save() {
        val current = _state.value
        val nameError = Validators.required(current.name, "Name")
        val phoneError = Validators.phone(current.phone, required = false)
        val emailError = Validators.email(current.email, required = false)
        if (nameError != null || phoneError != null || emailError != null) {
            _state.update {
                it.copy(nameError = nameError, phoneError = phoneError, emailError = emailError)
            }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            // Preserve the fields this form doesn't expose (logo, established year,
            // affiliation number) by editing a copy of what the server sent.
            val payload = (current.schoolInfo ?: SchoolInfoDto()).copy(
                name = current.name.trim(),
                address = current.address.trim(),
                phone = current.phone.trim(),
                email = current.email.trim(),
            )
            when (val result = settingsRepository.updateSchoolInfo(payload)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        isSaving = false,
                        schoolInfo = result.data,
                        message = UiMessage.success("School information saved."),
                    )
                }

                is ApiResult.Failure ->
                    _state.update { it.copy(isSaving = false, message = UiMessage.error(result.error)) }
            }
        }
    }
}

data class SettingsUiState(
    val schoolInfo: SchoolInfoDto? = null,
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val nameError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,
    val systemSettings: List<SystemSettingDto> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: AppError? = null,
    val message: UiMessage? = null,
)

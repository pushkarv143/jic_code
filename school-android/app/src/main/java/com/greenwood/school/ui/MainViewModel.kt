package com.greenwood.school.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.session.AccessStore
import com.greenwood.school.core.session.AuthEvent
import com.greenwood.school.core.session.AuthEventBus
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.dto.MyAccessDto
import com.greenwood.school.data.remote.dto.UserDto
import com.greenwood.school.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity-scoped state: is a session being restored, is the user signed in, and
 * has the session just expired.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val accessStore: AccessStore,
    private val authEventBus: AuthEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    /** Emits once per forced sign-out so navigation can reset the back stack. */
    val sessionExpired: Flow<Unit> = authEventBus.events
        .filterIsInstance<AuthEvent.SessionExpired>()
        .map { }

    init {
        viewModelScope.launch {
            // The Application already kicked off warmUp(); wait for it to land so the
            // first composition knows whether to show Login or the dashboard.
            sessionManager.warmUp()
            _state.value = _state.value.copy(isRestoringSession = false)
        }

        viewModelScope.launch {
            sessionManager.session.collect { session ->
                _state.value = _state.value.copy(
                    isSignedIn = session != null,
                    user = session?.user,
                    mustChangePassword = session?.mustChangePassword == true,
                )
                // Drives the live grants: signing in (or restoring a session on a warm
                // start) fetches them, signing out drops them so they are not left for
                // whoever signs in next. The login response carries a permission list
                // too, but it is a snapshot written to disk — see AccessStore.
                if (session != null) accessStore.refresh() else accessStore.clear()
            }
        }

        viewModelScope.launch {
            // A silent token swap rebuilds the caller's authorities server-side, so the
            // app re-reads them. The OkHttp authenticator that performs the swap is
            // synchronous and publishes an event instead of fetching.
            authEventBus.events
                .filterIsInstance<AuthEvent.TokensRefreshed>()
                .collect { accessStore.refresh() }
        }

        viewModelScope.launch {
            accessStore.settled.collect { settled ->
                _state.value = _state.value.copy(isAccessSettled = settled)
            }
        }

        viewModelScope.launch {
            accessStore.access.collect { access ->
                _state.value = _state.value.copy(access = access)
            }
        }

        viewModelScope.launch {
            // Refresh the cached profile in the background on a warm start. Roles and
            // the studentId/teacherId mapping can change server-side between sessions
            // and a stale copy would show the wrong menu.
            delay(PROFILE_REFRESH_DELAY_MS)
            if (sessionManager.currentUser != null) authRepository.refreshProfile()
        }
    }

    private companion object {
        const val PROFILE_REFRESH_DELAY_MS = 100L
    }
}

data class MainUiState(
    val isRestoringSession: Boolean = true,
    val isSignedIn: Boolean = false,
    val user: UserDto? = null,
    /**
     * True while this session is on a password the school generated.
     *
     * Drives which screen the graph opens: the change-password screen rather than
     * the dashboard. Not a security boundary — the API refuses every other endpoint
     * regardless — just the difference between a usable screen and a dashboard where
     * nothing loads.
     */
    val mustChangePassword: Boolean = false,
    /**
     * False until `GET /api/v1/me/access` has answered at least once.
     *
     * Screens with gated controls wait on this: before it lands every `can()` is
     * false, and painting from that would hide controls the user actually has.
     * Latches true and stays true, so a later background re-read updates the menu
     * in place rather than blanking the screen.
     */
    val isAccessSettled: Boolean = false,
    /** Live grants, modules and homeroom. Null until the first fetch lands. */
    val access: MyAccessDto? = null,
)

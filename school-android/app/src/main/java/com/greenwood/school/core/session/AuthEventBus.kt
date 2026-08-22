package com.greenwood.school.core.session

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-way channel from the network layer up to navigation.
 *
 * The OkHttp authenticator has no idea a UI exists, but when a refresh fails the
 * user must land back on Login. It emits [AuthEvent.SessionExpired] here and
 * `MainActivity` collects it — the Android equivalent of the web client's
 * `window.location.assign('/login')` in `axiosInstance.ts`.
 */
@Singleton
class AuthEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<AuthEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    /** Safe to call from any thread, including OkHttp's. */
    fun publish(event: AuthEvent) {
        _events.tryEmit(event)
    }
}

sealed interface AuthEvent {
    /** Refresh token rejected or absent — force a sign-out and show Login. */
    data object SessionExpired : AuthEvent

    /**
     * The access token was silently swapped for a fresh one.
     *
     * <p>Emitted so [AccessStore] can re-read `GET /api/v1/me/access`. The new token's
     * `PERM_*` authorities were rebuilt from `role_permissions` server-side, so this
     * is exactly the moment the app's cached idea of what the user may do can have
     * gone stale — and the authenticator that performs the swap is synchronous, so it
     * cannot do the fetch itself.
     */
    data object TokensRefreshed : AuthEvent
}

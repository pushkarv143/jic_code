package com.greenwood.school.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.security.KeystoreCrypto
import com.greenwood.school.data.remote.dto.JwtAuthResponseDto
import com.greenwood.school.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single owner of "who is signed in".
 *
 * Storage is DataStore, with the token pair encrypted by [KeystoreCrypto] first.
 * An in-memory mirror is kept because OkHttp's [okhttp3.Interceptor] and
 * [okhttp3.Authenticator] are synchronous — they cannot suspend to read DataStore
 * on every request. The mirror is hydrated once at startup and updated on write,
 * so the blocking path is only ever hit on a cold interceptor call before
 * [warmUp] has finished.
 */
@Singleton
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val crypto: KeystoreCrypto,
    private val json: Json,
) {

    private val _session = MutableStateFlow<UserSession?>(null)

    /** Null until [warmUp] completes, and null again after [clear]. */
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    val currentUser: UserDto? get() = _session.value?.user

    val isSignedIn: Flow<Boolean> = session.map { it != null }

    /**
     * Reads persisted credentials into memory. Called once from
     * [com.greenwood.school.SchoolApplication] before the first frame; safe to call again.
     */
    suspend fun warmUp() {
        if (_session.value != null) return
        _session.value = readFromDisk()
    }

    suspend fun save(auth: JwtAuthResponseDto) {
        val newSession = UserSession(
            accessToken = auth.accessToken,
            refreshToken = auth.refreshToken,
            tokenType = auth.tokenType.ifBlank { "Bearer" },
            user = auth.user,
        )
        _session.value = newSession
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS] = crypto.encrypt(newSession.accessToken)
            prefs[KEY_REFRESH] = crypto.encrypt(newSession.refreshToken)
            prefs[KEY_TOKEN_TYPE] = newSession.tokenType
            prefs[KEY_USER] = json.encodeToString(UserDto.serializer(), newSession.user)
        }
    }

    /** Replaces the cached profile after `/auth/me` or a profile edit, keeping tokens. */
    suspend fun updateUser(user: UserDto) {
        val current = _session.value ?: return
        _session.value = current.copy(user = user)
        dataStore.edit { prefs -> prefs[KEY_USER] = json.encodeToString(UserDto.serializer(), user) }
    }

    suspend fun clear() {
        _session.value = null
        dataStore.edit { it.clear() }
    }

    /**
     * Synchronous token access for the OkHttp layer. Falls back to a blocking disk
     * read only if the in-memory mirror has not been hydrated yet.
     */
    fun accessTokenBlocking(): String? =
        _session.value?.accessToken ?: runBlocking { readFromDisk() }?.also { _session.value = it }?.accessToken

    fun refreshTokenBlocking(): String? =
        _session.value?.refreshToken ?: runBlocking { readFromDisk() }?.also { _session.value = it }?.refreshToken

    /**
     * Swaps in a freshly refreshed token pair. Called from the OkHttp authenticator,
     * which is already off the main thread, so the blocking write is acceptable and
     * keeps the authenticator's single-flight guarantee intact.
     */
    fun saveRefreshedBlocking(auth: JwtAuthResponseDto) = runBlocking { save(auth) }

    fun clearBlocking() = runBlocking { clear() }

    private suspend fun readFromDisk(): UserSession? {
        val prefs = dataStore.data.first()
        val access = prefs[KEY_ACCESS]?.let(crypto::decrypt) ?: return null
        val refresh = prefs[KEY_REFRESH]?.let(crypto::decrypt) ?: return null
        val userJson = prefs[KEY_USER] ?: return null
        val user = runCatching { json.decodeFromString(UserDto.serializer(), userJson) }.getOrNull() ?: return null
        return UserSession(
            accessToken = access,
            refreshToken = refresh,
            tokenType = prefs[KEY_TOKEN_TYPE] ?: "Bearer",
            user = user,
        )
    }

    private companion object {
        val KEY_ACCESS = stringPreferencesKey("access_token")
        val KEY_REFRESH = stringPreferencesKey("refresh_token")
        val KEY_TOKEN_TYPE = stringPreferencesKey("token_type")
        val KEY_USER = stringPreferencesKey("user")
    }
}

data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val user: UserDto,
) {
    val role: Role get() = Role.from(user.role)

    /** The role's permission grants, as returned at login. */
    val permissions: Set<String> get() = user.permissions.toSet()

    /**
     * True when the signed-in role holds every one of [names].
     *
     * When the grant set is empty — a session cached before the backend started
     * returning permissions — this answers `true` and the caller falls back to
     * role checks alone. That is a display decision only: the API re-checks the
     * same grant on every request, so a permission the user does not actually
     * hold still yields 403 rather than data.
     */
    /**
     * The administrator is never gated by a permission grant, mirroring
     * AppConstants.ADMIN_OVERRIDE on the backend and usePermissions() on the web.
     */
    private val isAdmin: Boolean get() = role == Role.SUPER_ADMIN

    fun can(vararg names: String): Boolean =
        isAdmin || permissions.isEmpty() || names.all { it in permissions }

    fun canAny(vararg names: String): Boolean =
        isAdmin || permissions.isEmpty() || names.any { it in permissions }
}

package com.greenwood.school.core.session

import com.greenwood.school.core.common.Role
import com.greenwood.school.data.remote.api.AccessApi
import com.greenwood.school.data.remote.dto.HomeroomDto
import com.greenwood.school.data.remote.dto.MyAccessDto
import com.greenwood.school.data.remote.dto.moduleIsEnabled
import com.greenwood.school.data.remote.dto.permissionSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live authorization state — the single answer to "may this user do this?".
 *
 * <p>The Android counterpart of the web app's `AccessProvider`, and it exists for the
 * same reason: [UserSession.permissions] is the list embedded in the login response
 * and written to disk, so a permission an administrator grants or revokes — or a
 * module they switch off — never reaches a signed-in phone until the next login.
 * This re-reads `GET /api/v1/me/access` on every event that can change the answer:
 *
 * - **sign-in** — [refresh] is called after the session is saved;
 * - **token refresh** — the OkHttp authenticator swaps tokens silently, and the new
 *   token's authorities were rebuilt from `role_permissions` server-side, so that is
 *   precisely when the cached copy can have gone stale;
 * - **app start** — hydrated alongside [SessionManager.warmUp];
 * - **sign-out** — [clear] drops the grants rather than leaving them for whoever
 *   signs in next.
 *
 * <p><b>It fails closed.</b> Before the first successful fetch, and if a fetch fails
 * with nothing cached, [can] answers false for everything. The web app used to fall
 * back to the login-cached grants and treat an empty set as "unknown, so allow" —
 * convenient, but it briefly offers actions a role does not have, which is the whole
 * problem this replaces. [settled] is what screens gate on so nothing is drawn from
 * a guess.
 *
 * <p>None of this is a security boundary. Every endpoint re-checks the same grant on
 * every request. This only decides what the interface *offers*.
 */
@Singleton
class AccessStore @Inject constructor(
    private val accessApi: AccessApi,
) {

    private val _access = MutableStateFlow<MyAccessDto?>(null)
    val access: StateFlow<MyAccessDto?> = _access.asStateFlow()

    private val _settled = MutableStateFlow(false)
    val settled: StateFlow<Boolean> = _settled.asStateFlow()

    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    /**
     * Re-reads the caller's access. Safe to call repeatedly.
     *
     * <p>On failure the previous answer is kept rather than discarded: on the first
     * fetch there is nothing to keep so everything stays denied, but on a later
     * re-read holding grants a few seconds longer beats blanking a working screen
     * over one dropped request — and it cannot grant anything, since the API
     * re-checks every call.
     */
    suspend fun refresh() {
        _failed.value = false
        runCatching { accessApi.myAccess().data }
            .onSuccess { _access.value = it }
            .onFailure { _failed.value = true }
        _settled.value = true
    }

    /** Called on sign-out. */
    fun clear() {
        _access.value = null
        _settled.value = false
        _failed.value = false
    }

    private val current: MyAccessDto? get() = _access.value

    val role: Role get() = Role.from(current?.role)

    /** The section this user is class teacher of, or null. */
    val homeroom: HomeroomDto? get() = current?.homeroom

    /**
     * Holds a homeroom AND the MY_CLASS module is on. Read from the server rather
     * than inferred from a role — the CLASS_TEACHER role was retired because 27 of its 44 holders could not
     * back with an actual assignment.
     */
    val isClassTeacherOfOwnSection: Boolean
        get() = current.let { it != null && it.classTeacherOfOwnSection }

    val permissions: Set<String> get() = current.permissionSet

    /**
     * SUPER_ADMIN is never gated by a permission grant, mirroring
     * `AppConstants.ADMIN_OVERRIDE`. Keyed on the server-confirmed role, not the
     * cached one — trusting a locally stored role here would let tampered storage
     * unlock every control in the UI.
     */
    private val isAdmin: Boolean get() = current?.role == Role.SUPER_ADMIN.wireName

    /** True when the role grants every one of [names]. Strict: no grants, no actions. */
    fun can(vararg names: String): Boolean =
        isAdmin || (names.isNotEmpty() && names.all { it in permissions })

    /** True when the role grants at least one of [names]. */
    fun canAny(vararg names: String): Boolean =
        isAdmin || names.any { it in permissions }

    /** True when the named module is switched on. See [moduleIsEnabled] for the rule. */
    fun moduleEnabled(moduleKey: String): Boolean = current.moduleIsEnabled(moduleKey)
}

package com.greenwood.school.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * The section a user is class teacher of. Null on [MyAccessDto] for everyone else,
 * which includes most administrators.
 *
 * <p>This — not the CLASS_TEACHER role — is what decides whether "My Class" exists
 * for a user. On the seeded database 44 users hold that role while only 17 are
 * homeroom of anything, so the role alone would advertise an empty screen to 27 of
 * them.
 */
@Serializable
data class HomeroomDto(
    val sectionId: Long,
    val sectionName: String? = null,
    val classId: Long,
    val className: String? = null,
    val academicYearId: Long? = null,
    val academicYear: String? = null,
    val studentCount: Long = 0,
)

/**
 * Everything the app needs to decide what to offer the signed-in user, from
 * `GET /api/v1/me/access`.
 *
 * <p>Preferred over the permission list embedded in the login response, which is a
 * snapshot written to disk: a permission an administrator grants or revokes, or a
 * module they switch off, would not reach a signed-in phone until the next login.
 *
 * <p>Three separate things narrow what a user sees, and the app needs to tell them
 * apart:
 *
 * - [permissions] — the role's grants, already filtered server-side to drop
 *   anything belonging to a disabled module.
 * - [enabledModules] — which modules this school runs, so a whole menu section can
 *   be dropped rather than inferred from missing permissions.
 * - [homeroom] — the section this user is class teacher of. Not a permission: it is
 *   a row in `sections`.
 *
 * <p>[menus] is the navigation menu itself, read from the `menus` / `role_menus`
 * tables and already filtered by all three. The app used to hold its own copy of
 * the menu with the roles hard-coded in Destinations.kt, which meant changing who
 * saw what was a release of this app *and* the web one. Now the server decides and
 * this app supplies only the route and the icon for each key.
 */
@Serializable
data class MyAccessDto(
    val userId: Long,
    val username: String? = null,
    val role: String,
    val permissions: List<String> = emptyList(),
    val enabledModules: List<String> = emptyList(),
    val homeroom: HomeroomDto? = null,
    /**
     * This user's menu, as sections with their entries — already filtered.
     *
     * Empty for a role assigned nothing, which is a real configuration rather than
     * an error, and empty in the window before the first fetch lands. Neither case
     * falls back to a built-in menu: that fallback is what this replaced.
     */
    val menus: List<MenuNodeDto> = emptyList(),
    /**
     * True only when the user holds a homeroom assignment AND the MY_CLASS module is
     * enabled. Precomputed by the backend so every screen agrees on the rule instead
     * of each re-deriving it from three fields.
     */
    val classTeacherOfOwnSection: Boolean = false,
)

/**
 * One node of the server's menu tree — a section heading with [children], or an
 * entry with a [path].
 *
 * [menuKey] is what this app matches on, never [label] or [path]: the path is the
 * web app's route and means nothing here, while the key is stable across a rename.
 * Destinations.kt maps a key to the Compose route and icon; the server owns
 * everything else, including the order and whether the entry appears at all.
 */
@Serializable
data class MenuNodeDto(
    val id: Long,
    val menuKey: String,
    val label: String,
    val path: String? = null,
    val icon: String? = null,
    val i18nKey: String? = null,
    val sortOrder: Int = 0,
    val enabled: Boolean = true,
    val children: List<MenuNodeDto> = emptyList(),
    val moduleKey: String? = null,
    val requiredPermission: String? = null,
    val requiresHomeroom: Boolean = false,
)

/*
 * The access rules, as extensions so there is exactly one definition of each.
 *
 * AccessStore holds the state and the navigation graph reads it; both go through
 * these rather than each re-deriving the semantics, which is how the two would
 * otherwise drift.
 */

/** Effective grants. Empty means the role holds nothing — not "unknown". */
val MyAccessDto?.permissionSet: Set<String>
    get() = this?.permissions?.toSet() ?: emptySet()

/**
 * True when the named org module is switched on.
 *
 * Unknown modules read as enabled, mirroring `OrgModuleRepository.findDisabledModuleKeys`
 * on the backend: a module with no registry row must behave as on, so a migration
 * that adds one and forgets to register it does not black out that whole area. The
 * empty case also covers the window before the first fetch lands.
 */
fun MyAccessDto?.moduleIsEnabled(moduleKey: String): Boolean {
    val enabled = this?.enabledModules?.toSet() ?: emptySet()
    return enabled.isEmpty() || moduleKey in enabled
}

/** Holds a homeroom assignment AND the MY_CLASS module is on. Straight from the server. */
val MyAccessDto?.isClassTeacherOfOwnSection: Boolean
    get() = this?.classTeacherOfOwnSection == true

/**
 * Editing a student on the caller's own roster.
 *
 * <p>Omits `classId`, `sectionId`, `academicYearId` and `rollNumber`, and every
 * omission is deliberate:
 *
 * - the first three come from the homeroom assignment, so there is no field a class
 *   teacher could point at another class — an "edit my student" endpoint that
 *   accepted `sectionId` would quietly be an unaudited transfer endpoint;
 * - `rollNumber` is the student's position in their class, assigned by the server
 *   and unique per class, so nobody types it.
 *
 * <p>Null means "leave unchanged", so a screen can send only what it touched.
 */
@Serializable
data class MyClassStudentUpdateRequestDto(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val admissionDate: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val bloodGroup: String? = null,
    val religion: String? = null,
    val category: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
)

/**
 * Appointing a class post in the caller's own class.
 *
 * <p>No classId or sectionId — both come from the homeroom assignment. The student
 * must belong to that class, which the backend enforces: a class teacher cannot name
 * another class's student as their head boy.
 */
@Serializable
data class MyClassOfficialRequestDto(
    val studentId: Long,
    val role: String,
    /** Defaults to today server-side when omitted. */
    val fromDate: String? = null,
    val remarks: String? = null,
)

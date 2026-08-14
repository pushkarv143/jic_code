package com.greenwood.school.core.common

/**
 * The exact 11 roles seeded by `database/06_seed_reference_data.sql`, in seed order.
 *
 * The backend sends the role as a bare string on `UserDto.role`; [from] maps it and
 * falls back to [UNKNOWN] rather than throwing, so a role added server-side later
 * degrades to "authenticated with no extra menus" instead of crashing the app.
 */
enum class Role(val wireName: String, val label: String) {
    SUPER_ADMIN("SUPER_ADMIN", "Super Admin"),
    PRINCIPAL("PRINCIPAL", "Principal"),
    VICE_PRINCIPAL("VICE_PRINCIPAL", "Vice Principal"),
    TEACHER("TEACHER", "Teacher"),
    CLASS_TEACHER("CLASS_TEACHER", "Class Teacher"),
    ACCOUNTANT("ACCOUNTANT", "Accountant"),
    LIBRARIAN("LIBRARIAN", "Librarian"),
    RECEPTIONIST("RECEPTIONIST", "Receptionist"),
    STUDENT("STUDENT", "Student"),
    PARENT("PARENT", "Parent"),
    SECURITY_GUARD("SECURITY_GUARD", "Security Guard"),
    UNKNOWN("", "Member");

    companion object {
        fun from(raw: String?): Role =
            entries.firstOrNull { it.wireName.equals(raw?.trim(), ignoreCase = true) } ?: UNKNOWN

        /** SUPER_ADMIN / PRINCIPAL / VICE_PRINCIPAL — the web app's `MANAGEMENT` constant. */
        val MANAGEMENT = setOf(SUPER_ADMIN, PRINCIPAL, VICE_PRINCIPAL)

        val TEACHING = setOf(TEACHER, CLASS_TEACHER)

        /** Roles whose dashboard and menus are self-service rather than administrative. */
        val SELF_SERVICE = setOf(STUDENT, PARENT)
    }
}

val Role.isManagement: Boolean get() = this in Role.MANAGEMENT
val Role.isTeaching: Boolean get() = this in Role.TEACHING
val Role.isSelfService: Boolean get() = this in Role.SELF_SERVICE

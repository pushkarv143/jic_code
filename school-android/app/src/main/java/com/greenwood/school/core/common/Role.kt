package com.greenwood.school.core.common

/**
 * The roles the backend seeds, in seed order.
 *
 * The backend sends the role as a bare string on `UserDto.role`; [from] maps it and
 * falls back to [UNKNOWN] rather than throwing, so a role added server-side later
 * degrades to "authenticated with no extra menus" instead of crashing the app.
 *
 * CLASS_TEACHER used to sit between TEACHER and ACCOUNTANT. It was retired by
 * `database/21_single_teacher_role.sql`: it held exactly TEACHER's permissions plus
 * six, and 44 users carried it while only 17 headed a section, so the role said
 * something about a person that was not true of 27 of them. Being a class teacher
 * is `teachers.is_class_teacher` now, which the server turns into permissions —
 * so this app reads it from the access payload and never from the role.
 */
enum class Role(val wireName: String, val label: String) {
    SUPER_ADMIN("SUPER_ADMIN", "Super Admin"),
    PRINCIPAL("PRINCIPAL", "Principal"),
    VICE_PRINCIPAL("VICE_PRINCIPAL", "Vice Principal"),
    TEACHER("TEACHER", "Teacher"),
    ACCOUNTANT("ACCOUNTANT", "Accountant"),
    LIBRARIAN("LIBRARIAN", "Librarian"),
    RECEPTIONIST("RECEPTIONIST", "Receptionist"),
    STUDENT("STUDENT", "Student"),
    PARENT("PARENT", "Parent"),
    SECURITY_GUARD("SECURITY_GUARD", "Security Guard"),
    UNKNOWN("", "Member");

    companion object {
        /**
         * The name the retired CLASS_TEACHER role went by on the wire.
         *
         * Mapped to [TEACHER] rather than left to fall through to [UNKNOWN]. An
         * access token issued before the migration still carries the old name until
         * it expires, and a user holding one would otherwise be shown an empty shell
         * for up to the token's lifetime — a sign-out and back in they have no reason
         * to know they need. The row is gone from the database, so nothing new can
         * ever produce this value; it exists purely for tokens already in flight.
         */
        private const val RETIRED_CLASS_TEACHER = "CLASS_TEACHER"

        fun from(raw: String?): Role {
            val name = raw?.trim()
            if (RETIRED_CLASS_TEACHER.equals(name, ignoreCase = true)) {
                return TEACHER
            }
            return entries.firstOrNull { it.wireName.equals(name, ignoreCase = true) } ?: UNKNOWN
        }

        /** SUPER_ADMIN / PRINCIPAL / VICE_PRINCIPAL — the web app's `MANAGEMENT` constant. */
        val MANAGEMENT = setOf(SUPER_ADMIN, PRINCIPAL, VICE_PRINCIPAL)

        val TEACHING = setOf(TEACHER)

        /** Roles whose dashboard and menus are self-service rather than administrative. */
        val SELF_SERVICE = setOf(STUDENT, PARENT)
    }
}

val Role.isManagement: Boolean get() = this in Role.MANAGEMENT
val Role.isTeaching: Boolean get() = this in Role.TEACHING
val Role.isSelfService: Boolean get() = this in Role.SELF_SERVICE

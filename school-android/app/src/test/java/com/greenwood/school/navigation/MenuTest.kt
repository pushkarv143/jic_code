package com.greenwood.school.navigation

import com.greenwood.school.core.common.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the mobile menu to the web sidebar's role rules.
 *
 * If someone loosens a role set here, a teacher starts seeing Payroll and finds out
 * only when the backend returns 403 — these tests catch that at build time instead.
 */
class MenuTest {

    /**
     * The grants each role actually holds, from `database/06_seed_reference_data.sql`.
     *
     * Supplied because menu filtering is strict now: an entry declaring a permission
     * is hidden unless the user holds it, with no "empty set means unknown, so show
     * it" fallback. Passing the real grants makes these tests assert role *and*
     * permission together, which is what the app does — an empty set would only prove
     * that a role with no grants sees nothing.
     */
    private fun grantsFor(role: Role): Set<String> = when (role) {
        Role.SUPER_ADMIN -> ALL_GRANTS
        Role.PRINCIPAL -> setOf(
            "STUDENT_VIEW", "TEACHER_VIEW", "ATTENDANCE_VIEW", "MATERIAL_VIEW",
            "PAYROLL_VIEW", "REPORT_VIEW", "ADMISSION_VIEW", "USER_VIEW", "SETTINGS_VIEW",
        )
        Role.VICE_PRINCIPAL -> setOf(
            "STUDENT_VIEW", "TEACHER_VIEW", "ATTENDANCE_VIEW", "MATERIAL_VIEW", "REPORT_VIEW",
        )
        Role.TEACHER -> setOf("STUDENT_VIEW", "ATTENDANCE_VIEW", "MATERIAL_VIEW", "REPORT_VIEW")
        Role.CLASS_TEACHER -> setOf(
            "STUDENT_VIEW", "ATTENDANCE_VIEW", "MATERIAL_VIEW", "REPORT_VIEW",
            "MY_CLASS_VIEW", "MY_CLASS_ROSTER_MANAGE", "MY_CLASS_OFFICIALS_MANAGE",
        )
        Role.ACCOUNTANT -> setOf("STUDENT_VIEW", "REPORT_VIEW")
        Role.LIBRARIAN -> setOf("STUDENT_VIEW", "TEACHER_VIEW")
        Role.RECEPTIONIST -> setOf("STUDENT_VIEW", "ADMISSION_VIEW", "REPORT_VIEW")
        Role.STUDENT -> setOf("STUDENT_VIEW", "ATTENDANCE_VIEW", "MATERIAL_VIEW")
        Role.PARENT -> setOf("STUDENT_VIEW", "ATTENDANCE_VIEW", "MATERIAL_VIEW")
        Role.SECURITY_GUARD, Role.UNKNOWN -> emptySet()
    }

    /** Every permission a menu entry can ask for — SUPER_ADMIN holds them all. */
    private val ALL_GRANTS = setOf(
        "STUDENT_VIEW", "TEACHER_VIEW", "ATTENDANCE_VIEW", "MATERIAL_VIEW", "PAYROLL_VIEW",
        "REPORT_VIEW", "ADMISSION_VIEW", "USER_VIEW", "SETTINGS_VIEW", "MY_CLASS_VIEW",
    )

    private fun routesFor(
        role: Role,
        granted: Set<String> = grantsFor(role),
        moduleEnabled: (String) -> Boolean = { true },
        hasHomeroom: Boolean = false,
    ): Set<String> =
        fullMenuForRole(role, granted, moduleEnabled, hasHomeroom)
            .flatMap { section -> section.entries.map { it.route } }
            .toSet()

    @Test
    fun `management sees the administrative modules`() {
        val routes = routesFor(Role.PRINCIPAL)

        assertTrue(Routes.STUDENTS in routes)
        assertTrue(Routes.TEACHERS in routes)
        assertTrue(Routes.PAYROLL in routes)
        assertTrue(Routes.REPORTS in routes)
        assertTrue(Routes.ADMISSIONS in routes)
    }

    @Test
    fun `a teacher cannot see payroll, users or settings`() {
        val routes = routesFor(Role.TEACHER)

        assertTrue(Routes.STUDENTS in routes)
        assertTrue(Routes.ATTENDANCE in routes)
        assertFalse(Routes.PAYROLL in routes)
        assertFalse(Routes.USERS in routes)
        assertFalse(Routes.SETTINGS in routes)
        assertFalse(Routes.REPORTS in routes)
    }

    @Test
    fun `a student sees only self-service destinations`() {
        val routes = routesFor(Role.STUDENT)

        assertTrue(Routes.ATTENDANCE in routes)
        assertTrue(Routes.FEES in routes)
        assertTrue(Routes.ASSIGNMENTS in routes)
        assertTrue(Routes.LEAVE in routes)

        // Students is deliberately visible: one path serves two pages, the directory
        // for anyone who supervises students and the student's own record for a
        // STUDENT, so the menu stays identical across roles. AppNavHost redirects
        // them to /students/{id}. Both clients agree — see the same grant in the
        // web app's navConfig, and StudentsIndexRoute that it pairs with.
        // It is self-service for a student, which is what this test is about.
        assertTrue(Routes.STUDENTS in routes)

        // Teachers has no such split for a student — it is management and teaching
        // only, so it stays hidden.
        assertFalse(Routes.TEACHERS in routes)
        assertFalse(Routes.PAYROLL in routes)
    }

    @Test
    fun `only a parent sees My Children`() {
        assertTrue(Routes.MY_CHILDREN in routesFor(Role.PARENT))
        assertFalse(Routes.MY_CHILDREN in routesFor(Role.STUDENT))
        assertFalse(Routes.MY_CHILDREN in routesFor(Role.SUPER_ADMIN))
    }

    @Test
    fun `every role can apply for leave`() {
        // The web navConfig deliberately leaves `roles` off this entry.
        Role.entries.filter { it != Role.UNKNOWN }.forEach { role ->
            assertTrue("$role should see Leave", Routes.LEAVE in routesFor(role))
        }
    }

    @Test
    fun `the librarian gets the library but nothing else administrative`() {
        val routes = routesFor(Role.LIBRARIAN)

        assertTrue(Routes.LIBRARY in routes)
        assertFalse(Routes.FEES in routes)
        assertFalse(Routes.PAYROLL in routes)
        assertFalse(Routes.STUDENTS in routes)
    }

    @Test
    fun `an unknown role still gets the always-visible entries and nothing more`() {
        val routes = routesFor(Role.UNKNOWN)

        // Dashboard, Leave, notices, calendar, notifications, profile carry no role filter.
        assertTrue(Routes.DASHBOARD in routes)
        assertTrue(Routes.PROFILE in routes)
        assertFalse(Routes.STUDENTS in routes)
        assertFalse(Routes.SETTINGS in routes)
    }

    @Test
    fun `every menu entry the user can tap has a registered route`() {
        // menuForRole is what the hubs render; nothing it returns may be missing
        // from the nav graph, or the tap crashes.
        Role.entries.forEach { role ->
            menuForRole(role).flatMap { it.entries }.forEach { entry ->
                assertTrue(
                    "${entry.label} (${entry.route}) is in a menu but not in IMPLEMENTED_ROUTES",
                    entry.route in IMPLEMENTED_ROUTES,
                )
            }
        }
    }

    @Test
    fun `route builders produce paths that match their patterns`() {
        assertEquals("students/42", Routes.studentDetail(42))
        assertEquals("teachers/7", Routes.teacherDetail(7))
        assertEquals("classes/3", Routes.classDetail(3))
        assertEquals("exams/9/schedules/11/marks", Routes.marksEntry(9, 11))
        // -1 is the "new record" sentinel StudentFormViewModel checks for.
        assertEquals("student-form?studentId=-1", Routes.studentForm())
        assertEquals("student-form?studentId=5", Routes.studentForm(5))
    }

    @Test
    fun `the form routes cannot be mistaken for detail routes`() {
        // "students/form" would otherwise match "students/{studentId}".
        assertFalse(Routes.studentForm().startsWith("students/"))
        assertFalse(Routes.teacherForm().startsWith("teachers/"))
    }

    /* ---- The three gates added with live access ------------------------------ */

    /**
     * My Class follows the homeroom *assignment*, not the CLASS_TEACHER role.
     *
     * On the seeded database 44 users hold that role and only 17 hold an assignment,
     * so gating on the role would advertise an empty screen to 27 people.
     */
    @Test
    fun `My Class needs a homeroom assignment, not the class teacher role`() {
        assertTrue(Routes.MY_CLASS in routesFor(Role.CLASS_TEACHER, hasHomeroom = true))
        assertFalse(Routes.MY_CLASS in routesFor(Role.CLASS_TEACHER, hasHomeroom = false))

        // A plain TEACHER put in sections.class_teacher_id genuinely is a class
        // teacher, so the entry follows the assignment rather than the label.
        assertTrue(
            Routes.MY_CLASS in routesFor(
                Role.TEACHER,
                granted = grantsFor(Role.TEACHER) + "MY_CLASS_VIEW",
                hasHomeroom = true,
            ),
        )
    }

    /** Switching a module off hides its entry for everyone, the administrator included. */
    @Test
    fun `a disabled module hides its entry even for the administrator`() {
        val hostelOff = { key: String -> key != "HOSTEL" }

        assertFalse(Routes.HOSTEL in routesFor(Role.SUPER_ADMIN, moduleEnabled = hostelOff))
        assertFalse(Routes.HOSTEL in routesFor(Role.PRINCIPAL, moduleEnabled = hostelOff))
        // Everything else is untouched by one module going dark.
        assertTrue(Routes.STUDENTS in routesFor(Role.SUPER_ADMIN, moduleEnabled = hostelOff))
    }

    /**
     * Strict permission filtering: no grants, no permission-gated entries.
     *
     * This replaces a fallback that treated an empty grant set as "unknown" and showed
     * the entry anyway, which briefly offered actions a role does not have.
     */
    @Test
    fun `a role with no grants sees no permission-gated entries`() {
        val routes = routesFor(Role.TEACHER, granted = emptySet())

        assertFalse(Routes.STUDENTS in routes)
        assertFalse(Routes.ATTENDANCE in routes)
        // Leave declares no permission, so it survives — as it should.
        assertTrue(Routes.LEAVE in routes)
    }

    /** SUPER_ADMIN bypasses the permission gate, but never the module gate. */
    @Test
    fun `the administrator bypasses permissions but not modules`() {
        assertTrue(Routes.STUDENTS in routesFor(Role.SUPER_ADMIN, granted = emptySet()))
        assertFalse(
            Routes.STUDENTS in routesFor(
                Role.SUPER_ADMIN,
                granted = emptySet(),
                moduleEnabled = { it != "STUDENT" },
            ),
        )
    }
}

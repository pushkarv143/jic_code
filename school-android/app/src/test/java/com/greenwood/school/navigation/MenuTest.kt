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

    private fun routesFor(role: Role): Set<String> =
        fullMenuForRole(role).flatMap { section -> section.entries.map { it.route } }.toSet()

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
        assertFalse(Routes.STUDENTS in routes)
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
}

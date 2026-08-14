package com.greenwood.school.core.common

import com.greenwood.school.ui.common.PagedListState
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.Paged
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic tests. `Validators.email`/`phone` lean on `android.util.Patterns`, so
 * those two cases live in the instrumented suite; everything below is JVM-safe.
 */
class ValidatorsTest {

    @Test
    fun `password policy matches the seeded accounts`() {
        // Both seeded passwords must pass, or every demo account would be unable to
        // change its password through the app.
        assertNull(Validators.password("Admin@123"))
        assertNull(Validators.password("Password@123"))
    }

    @Test
    fun `password policy rejects each missing character class`() {
        assertEquals("Password is required", Validators.password(""))
        assertEquals("Password must be at least 8 characters", Validators.password("Ab@1"))
        assertEquals("Include at least one uppercase letter", Validators.password("password@1"))
        assertEquals("Include at least one lowercase letter", Validators.password("PASSWORD@1"))
        assertEquals("Include at least one number", Validators.password("Password@"))
        assertEquals("Include at least one special character", Validators.password("Password1"))
    }

    @Test
    fun `confirmPassword only passes on an exact match`() {
        assertNull(Validators.confirmPassword("Admin@123", "Admin@123"))
        assertNotNull(Validators.confirmPassword("Admin@123", "admin@123"))
        assertNotNull(Validators.confirmPassword("Admin@123", ""))
    }

    @Test
    fun `positiveNumber guards the amount fields`() {
        assertNull(Validators.positiveNumber("1500.50", "Amount"))
        assertNull(Validators.positiveNumber("0", "Amount", allowZero = true))
        assertEquals("Amount must be greater than zero", Validators.positiveNumber("0", "Amount"))
        assertEquals("Amount cannot be negative", Validators.positiveNumber("-5", "Amount"))
        assertEquals("Amount must be a number", Validators.positiveNumber("abc", "Amount"))
    }

    @Test
    fun `firstError returns the first failing check only`() {
        assertEquals(
            "second",
            Validators.firstError(null, "second", "third"),
        )
        assertNull(Validators.firstError(null, null))
    }
}

class FormattersTest {

    @Test
    fun `currency uses Indian lakh grouping`() {
        assertEquals("₹1,234.00", Formatters.currency(1234.0))
        assertEquals("₹12,34,567.89", Formatters.currency(1234567.89))
        assertEquals("₹-1,000.00", Formatters.currency(-1000.0))
        assertEquals(Formatters.PLACEHOLDER, Formatters.currency(null))
    }

    @Test
    fun `dates fall back to a placeholder instead of throwing`() {
        assertEquals("13 Aug 2026", Formatters.date("2026-08-13"))
        assertEquals(Formatters.PLACEHOLDER, Formatters.date("not-a-date"))
        assertEquals(Formatters.PLACEHOLDER, Formatters.date(null))
    }

    @Test
    fun `a LocalDateTime string still renders as a date`() {
        assertEquals("13 Aug 2026", Formatters.date("2026-08-13T16:03:44.404153844"))
    }

    @Test
    fun `humanizeEnum turns wire values into labels`() {
        assertEquals("Super Admin", Formatters.humanizeEnum("SUPER_ADMIN"))
        assertEquals("Half Day", Formatters.humanizeEnum("HALF_DAY"))
        assertEquals(Formatters.PLACEHOLDER, Formatters.humanizeEnum(null))
    }

    @Test
    fun `personName tolerates the nullable name columns`() {
        assertEquals("Asha Rao", Formatters.personName("Asha", "Rao"))
        assertEquals("Asha", Formatters.personName("Asha", null))
        assertEquals("ADM-001", Formatters.personName(null, null, "ADM-001"))
        assertEquals(Formatters.PLACEHOLDER, Formatters.personName(null, null))
    }
}

class PagedListStateTest {

    @Test
    fun `page zero replaces and later pages append`() {
        val first = PagedListState<String>().applyPage(Paged(listOf("a", "b"), 0, 4, 2, false))
        assertEquals(listOf("a", "b"), first.items)

        val second = first.applyPage(Paged(listOf("c", "d"), 1, 4, 2, true))
        assertEquals(listOf("a", "b", "c", "d"), second.items)
        assertTrue(second.isLastPage)
        assertTrue(!second.canLoadMore)
    }

    @Test
    fun `a refresh replaces rather than duplicating`() {
        val loaded = PagedListState<String>().applyPage(Paged(listOf("a", "b"), 0, 2, 1, true))
        val refreshed = loaded.startRefresh().applyPage(Paged(listOf("a"), 0, 1, 1, true))

        assertEquals(listOf("a"), refreshed.items)
    }

    @Test
    fun `a failed first page is fatal but a failed append is not`() {
        val emptyFailure = PagedListState<String>().startRefresh().applyError(AppError.Network("offline"))
        assertNotNull(emptyFailure.error)
        assertNull(emptyFailure.appendError)

        val loaded = PagedListState<String>().applyPage(Paged(listOf("a"), 0, 10, 5, false))
        val appendFailure = loaded.startAppend().applyError(AppError.Network("offline"))
        // Content stays on screen; only the footer shows the problem.
        assertEquals(listOf("a"), appendFailure.items)
        assertNull(appendFailure.error)
        assertNotNull(appendFailure.appendError)
    }

    @Test
    fun `canLoadMore is false while a load is already in flight`() {
        val loaded = PagedListState<String>().applyPage(Paged(listOf("a"), 0, 10, 5, false))
        assertTrue(loaded.canLoadMore)
        assertTrue(!loaded.startAppend().canLoadMore)
        assertTrue(!loaded.startRefresh().canLoadMore)
    }
}

class RoleTest {

    @Test
    fun `every seeded role maps from its wire name`() {
        assertEquals(Role.SUPER_ADMIN, Role.from("SUPER_ADMIN"))
        assertEquals(Role.CLASS_TEACHER, Role.from("CLASS_TEACHER"))
        assertEquals(Role.SECURITY_GUARD, Role.from("SECURITY_GUARD"))
    }

    @Test
    fun `an unrecognised role degrades instead of throwing`() {
        // A role added server-side must not crash an older build.
        assertEquals(Role.UNKNOWN, Role.from("REGISTRAR"))
        assertEquals(Role.UNKNOWN, Role.from(null))
    }

    @Test
    fun `role groupings match the web navConfig`() {
        assertTrue(Role.PRINCIPAL.isManagement)
        assertTrue(Role.VICE_PRINCIPAL.isManagement)
        assertTrue(!Role.TEACHER.isManagement)
        assertTrue(Role.CLASS_TEACHER.isTeaching)
        assertTrue(Role.PARENT.isSelfService)
    }
}

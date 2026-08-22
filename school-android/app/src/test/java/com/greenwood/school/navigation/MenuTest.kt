package com.greenwood.school.navigation

import com.greenwood.school.data.remote.dto.MenuNodeDto
import com.greenwood.school.data.remote.dto.MyAccessDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the app still decides about the menu, now that the server decides the rest.
 *
 * <p>This file used to assert the role rules — that a teacher saw no Payroll, that
 * a librarian saw no Students — because those rules lived here, in \`roles\` arrays
 * on every menu entry. They live in \`menus\` / \`role_menus\` now and are tested
 * server-side, where they are enforced. Asserting them again from a hard-coded copy
 * would only prove the copy still existed.
 *
 * <p>What is left is the app's half of the contract, and it is worth locking down:
 * a menu key must map to a screen that exists, an unknown key must be dropped
 * rather than crash, and a menu must never be invented when the server sent none.
 */
class MenuTest {

    private fun entry(key: String, label: String = key) =
        MenuNodeDto(id = 1, menuKey = key, label = label, path = "/app/x")

    private fun section(key: String, label: String, children: List<MenuNodeDto>) =
        MenuNodeDto(id = 99, menuKey = key, label = label, children = children)

    private fun accessOf(vararg sections: MenuNodeDto) =
        MyAccessDto(userId = 1, role = "TEACHER", menus = sections.toList())

    /* ---- the catalogue --------------------------------------------------- */

    @Test
    fun `every menu in the catalogue points at a route the graph registers`() {
        // Navigating to a route the NavHost does not know throws at runtime, so a
        // catalogue entry for an unwired screen is a crash waiting for a tap.
        MENU_CATALOGUE.values.forEach { entry ->
            assertTrue(
                "${entry.key} -> ${entry.route} is in MENU_CATALOGUE but not IMPLEMENTED_ROUTES",
                entry.route in IMPLEMENTED_ROUTES,
            )
        }
    }

    @Test
    fun `the catalogue is keyed consistently with its entries`() {
        MENU_CATALOGUE.forEach { (key, entry) ->
            assertEquals("MENU_CATALOGUE key disagrees with the entry it holds", key, entry.key)
        }
    }

    @Test
    fun `chat is deliberately absent`() {
        // The web page is a placeholder over seeded conversations with no controller
        // behind it. The menu row exists and is seeded disabled; there is nothing
        // here to navigate to even if it were sent.
        assertFalse("CHAT" in MENU_CATALOGUE)
    }

    /* ---- translating the server's menu ---------------------------------- */

    @Test
    fun `the server's label wins over the built-in one`() {
        val menu = menuFromAccess(
            accessOf(section("SECTION_ACADEMICS", "Teaching", listOf(entry("STUDENTS", "Pupils")))),
        )

        // A school that calls them pupils gets "Pupils" without an app release.
        assertEquals("Teaching", menu.single().title)
        assertEquals("Pupils", menu.single().entries.single().label)
    }

    @Test
    fun `the route and icon come from the app, not the server`() {
        val menu = menuFromAccess(
            accessOf(section("SECTION_ACADEMICS", "Academics", listOf(entry("STUDENTS")))),
        )

        // `menus.path` is the *web* route and means nothing here.
        assertEquals(Routes.STUDENTS, menu.single().entries.single().route)
        assertEquals(MENU_CATALOGUE["STUDENTS"]!!.icon, menu.single().entries.single().icon)
    }

    @Test
    fun `a key this app has no screen for is skipped`() {
        val menu = menuFromAccess(
            accessOf(
                section(
                    "SECTION_ACADEMICS", "Academics",
                    listOf(entry("STUDENTS"), entry("CHAT"), entry("SOMETHING_NEW")),
                ),
            ),
        )

        // The phone does not implement every screen the web app has. Offering a menu
        // that navigates nowhere is worse than omitting it - and a *new* key from a
        // newer server must not crash an older app.
        assertEquals(listOf("STUDENTS"), menu.single().entries.map { it.key })
    }

    @Test
    fun `a section left with nothing renderable is dropped`() {
        val menu = menuFromAccess(
            accessOf(
                section("SECTION_ACADEMICS", "Academics", listOf(entry("STUDENTS"))),
                section("SECTION_COMMUNICATION", "Communication", listOf(entry("CHAT"))),
            ),
        )

        // A heading over an empty list reads as a failure to load.
        assertEquals(listOf("SECTION_ACADEMICS"), menu.map { it.key })
    }

    @Test
    fun `the section order is the server's`() {
        val menu = menuFromAccess(
            accessOf(
                section("SECTION_ACCOUNT", "Account", listOf(entry("PROFILE"))),
                section("SECTION_ACADEMICS", "Academics", listOf(entry("STUDENTS"))),
            ),
        )

        // sort_order is a column, so reordering the sidebar is an UPDATE.
        assertEquals(listOf("SECTION_ACCOUNT", "SECTION_ACADEMICS"), menu.map { it.key })
    }

    /* ---- no menu means no menu ------------------------------------------ */

    @Test
    fun `no access yet means no menu, not a built-in one`() {
        // Before the first /me/access lands. The shell holds the splash until it
        // settles, so this is not drawn - but falling back to a hard-coded menu is
        // exactly what this whole change removed.
        assertTrue(menuFromAccess(null).isEmpty())
    }

    @Test
    fun `a role assigned nothing sees nothing`() {
        // A real configuration, not an error: a role that exists only to hold an
        // account. There is no floor of "everyone gets a dashboard" in the client.
        assertTrue(menuFromAccess(accessOf()).isEmpty())
    }
}

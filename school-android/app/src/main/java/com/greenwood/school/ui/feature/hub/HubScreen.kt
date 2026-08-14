package com.greenwood.school.ui.feature.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
// The List-taking `items` overload is an extension, not a member of LazyListScope —
// without this import the call below resolves to nothing.
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenwood.school.core.common.Role
import com.greenwood.school.navigation.MenuEntry
import com.greenwood.school.navigation.MenuSection
import com.greenwood.school.navigation.Routes
import com.greenwood.school.navigation.menuForRole
import com.greenwood.school.ui.components.AppCard
import com.greenwood.school.ui.components.EmptyView

/**
 * A hub is one bottom-bar tab rendered as a menu of destinations — the mobile
 * stand-in for a slice of the web sidebar.
 *
 * Which sections land in which tab is decided here rather than in the nav graph so
 * that adding a menu entry to `MENU_SECTIONS` automatically surfaces it in the
 * right tab without touching navigation code.
 */
@Composable
fun HubScreen(
    role: Role,
    tabRoute: String,
    onNavigate: (String) -> Unit,
    onSignOut: (() -> Unit)? = null,
    /** The signed-in role's permission grants; empty falls back to role-only filtering. */
    permissions: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    val sections = menuForRole(role, permissions).filter { it.title in sectionsFor(tabRoute) }
        // "Dashboard" is the Home tab itself; showing it again inside a hub is noise.
        .map { section -> section.copy(entries = section.entries.filterNot { it.route == Routes.DASHBOARD }) }
        .filter { it.entries.isNotEmpty() }

    if (sections.isEmpty()) {
        EmptyView(
            title = "Nothing here for your role",
            message = "Your account doesn't have access to anything in this section.",
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sections.forEach { section ->
            item(key = "header-${section.title}") { SectionHeader(section) }
            items(section.entries, key = { it.route }) { entry ->
                MenuRow(entry = entry, onClick = { onNavigate(entry.route) })
            }
        }

        if (onSignOut != null) {
            item(key = "sign-out") {
                Spacer(Modifier.padding(top = 8.dp))
                TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(section: MenuSection) {
    Text(
        text = section.title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun MenuRow(entry: MenuEntry, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                entry.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(entry.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Maps a bottom tab to the web sidebar groups it owns. "More" is deliberately the
 * catch-all so nothing from the web nav can be lost by omission.
 */
private fun sectionsFor(tabRoute: String): Set<String> = when (tabRoute) {
    Routes.ACADEMICS_HUB -> setOf("Academics")
    Routes.ADMIN_HUB -> setOf("Administration")
    else -> setOf("Overview", "Communication", "Insights", "Account")
}

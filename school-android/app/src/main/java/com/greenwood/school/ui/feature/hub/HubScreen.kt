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
import com.greenwood.school.navigation.MenuEntry
import com.greenwood.school.navigation.MenuSection
import com.greenwood.school.navigation.Routes
import com.greenwood.school.data.remote.dto.MyAccessDto
import com.greenwood.school.navigation.menuFromAccess
import com.greenwood.school.ui.components.AppCard
import com.greenwood.school.ui.components.EmptyView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.greenwood.school.ui.theme.BrandIndigo
import com.greenwood.school.ui.theme.BrandIndigoLight

/**
 * A hub is one bottom-bar tab rendered as a menu of destinations — the mobile
 * stand-in for a slice of the web sidebar.
 *
 * Which sections land in which tab is decided here rather than in the nav graph, so
 * a menu added to the `menus` table surfaces in the right tab with no code change
 * at all — not even a release. The tab mapping is by section key, so renaming a
 * heading does not move its contents.
 */
@Composable
fun HubScreen(
    tabRoute: String,
    onNavigate: (String) -> Unit,
    onSignOut: (() -> Unit)? = null,
    /**
     * What the server says this user may see, from `GET /api/v1/me/access`.
     *
     * Carries the menu itself, already filtered by the role's assignment, the
     * enabled modules, the homeroom and the permissions. Not the login response's
     * copy: that is a snapshot written to disk and goes stale the moment an
     * administrator changes anything. Null only before the first fetch settles, and
     * the shell holds the splash until then.
     */
    access: MyAccessDto? = null,
    modifier: Modifier = Modifier,
) {
    val sections = menuFromAccess(access)
        .filter { it.key in sectionsFor(tabRoute) }
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
                Spacer(Modifier.height(12.dp))
                AppCard(modifier = Modifier.fillMaxWidth(), onClick = onSignOut) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Sign out",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(section: MenuSection) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 16.dp, bottom = 6.dp, end = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = section.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun MenuRow(entry: MenuEntry, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Gradient icon box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BrandIndigo, BrandIndigoLight)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    entry.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Maps a bottom tab to the web sidebar groups it owns. "More" is deliberately the
 * catch-all so nothing from the web nav can be lost by omission.
 */
private fun sectionsFor(tabRoute: String): Set<String> = when (tabRoute) {
    Routes.ACADEMICS_HUB -> setOf("SECTION_ACADEMICS")
    Routes.ADMIN_HUB -> setOf("SECTION_ADMINISTRATION")
    // "More" is the catch-all: anything not claimed by a named tab lands here, so a
    // section added to the table appears rather than vanishing.
    else -> setOf(
        "SECTION_OVERVIEW",
        "SECTION_COMMUNICATION",
        "SECTION_INSIGHTS",
        "SECTION_ACCOUNT",
    )
}

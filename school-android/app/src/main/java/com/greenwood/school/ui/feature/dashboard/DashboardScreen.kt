package com.greenwood.school.ui.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.isManagement
import com.greenwood.school.ui.components.AppCard
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.OfflineBanner
import com.greenwood.school.ui.components.SectionCard
import com.greenwood.school.ui.components.StatCard
import com.greenwood.school.ui.theme.StatusPalette

/**
 * The mobile dashboard. Stat tiles two-per-row instead of the web's six-across
 * strip, then the same "recent notices" and "upcoming events" widgets the web
 * dashboard carries.
 *
 * Charts from the web version (attendance-by-class bar, fee-by-month line) are
 * represented here as the aggregate figure plus a link into Reports — a 12-series
 * chart on a 5-inch screen is noise, and Reports is where the breakdown belongs.
 */
@Composable
fun DashboardScreen(
    onOpenNotices: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFees: () -> Unit,
    onOpenAttendance: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        OfflineBanner(visible = !isOnline)

        if (state.isLoading) {
            FullScreenLoader("Loading dashboard…")
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = greeting(state.greetingName),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = state.role.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    }
                }
            }

            item { StatGrid(state, onOpenFees = onOpenFees, onOpenAttendance = onOpenAttendance) }

            if (state.notices.isNotEmpty()) {
                item {
                    SectionCard(
                        title = "Recent notices",
                        action = {
                            IconButton(onClick = onOpenNotices) {
                                Icon(Icons.Outlined.Campaign, contentDescription = "All notices")
                            }
                        },
                    ) {
                        state.notices.forEach { notice ->
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Text(
                                    notice.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    notice.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    Formatters.dateTime(notice.publishedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (state.upcomingEvents.isNotEmpty()) {
                item {
                    SectionCard(
                        title = "Upcoming events",
                        action = {
                            IconButton(onClick = onOpenCalendar) {
                                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar")
                            }
                        },
                    ) {
                        state.upcomingEvents.forEach { event ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(event.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        Formatters.humanizeEnum(event.eventType),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    Formatters.date(event.eventDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (state.notices.isEmpty() && state.upcomingEvents.isEmpty() && !state.hasAnyMetric) {
                item {
                    AppCard(Modifier.fillMaxWidth()) {
                        Text(
                            "Nothing to show yet. Your dashboard fills up as the school " +
                                "publishes notices, events and records for your account.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Two tiles per row — the widest layout that keeps a rupee figure legible on a phone. */
@Composable
private fun StatGrid(
    state: DashboardUiState,
    onOpenFees: () -> Unit,
    onOpenAttendance: () -> Unit,
) {
    val tiles = buildList {
        state.analytics?.let { analytics ->
            add(
                Tile("Active students", Formatters.number(analytics.totalActiveStudents), Icons.Outlined.School)
            )
            add(
                Tile("Active teachers", Formatters.number(analytics.totalActiveTeachers), Icons.Outlined.Badge)
            )
            add(
                Tile(
                    label = "Average attendance",
                    value = Formatters.percent(analytics.averageAttendancePercentage),
                    icon = Icons.Outlined.EventAvailable,
                    accent = StatusPalette.positive,
                    onClick = onOpenAttendance,
                ),
            )
            add(
                Tile(
                    label = "Fees collected",
                    value = Formatters.currency(analytics.totalFeeCollected),
                    icon = Icons.Outlined.Paid,
                    accent = StatusPalette.positive,
                    onClick = onOpenFees,
                ),
            )
            add(
                Tile(
                    label = "Fees outstanding",
                    value = Formatters.currency(analytics.totalFeeOutstanding),
                    icon = Icons.Outlined.Warning,
                    accent = StatusPalette.caution,
                    onClick = onOpenFees,
                ),
            )
        }

        state.payroll?.let { payroll ->
            add(
                Tile(
                    label = "Payroll pending this month",
                    value = Formatters.currency(payroll.totalPending),
                    icon = Icons.Outlined.AccountBalanceWallet,
                    accent = StatusPalette.caution,
                ),
            )
        }

        state.library?.let { library ->
            add(Tile("Books issued", Formatters.number(library.issuedCount), Icons.Outlined.MenuBook))
            add(
                Tile(
                    label = "Overdue books",
                    value = Formatters.number(library.overdueCount),
                    icon = Icons.Outlined.Warning,
                    accent = StatusPalette.negative,
                ),
            )
        }

        state.myAttendance?.let { summary ->
            add(
                Tile(
                    label = "My attendance",
                    value = Formatters.percent(summary.percentage),
                    icon = Icons.Outlined.EventAvailable,
                    accent = if (summary.percentage >= MIN_HEALTHY_ATTENDANCE) {
                        StatusPalette.positive
                    } else {
                        StatusPalette.caution
                    },
                    onClick = onOpenAttendance,
                ),
            )
        }

        if (state.role in Role.SELF_SERVICE) {
            add(
                Tile(
                    label = "My outstanding fees",
                    value = Formatters.currency(state.myOutstandingFees),
                    icon = Icons.Outlined.Paid,
                    accent = if (state.myOutstandingFees > 0) StatusPalette.caution else StatusPalette.positive,
                    onClick = onOpenFees,
                ),
            )
        }
    }

    if (tiles.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tile ->
                    StatCard(
                        label = tile.label,
                        value = tile.value,
                        icon = tile.icon,
                        accent = tile.accent,
                        onClick = tile.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keeps a lone trailing tile at half width instead of stretching it.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

private data class Tile(
    val label: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: androidx.compose.ui.graphics.Color = com.greenwood.school.ui.theme.BrandIndigo,
    val onClick: (() -> Unit)? = null,
)

private val DashboardUiState.hasAnyMetric: Boolean
    get() = analytics != null || payroll != null || library != null || myAttendance != null

private fun greeting(firstName: String): String =
    if (firstName.isBlank()) "Welcome back" else "Hello, $firstName"

private const val MIN_HEALTHY_ATTENDANCE = 75.0

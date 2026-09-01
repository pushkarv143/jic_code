package com.greenwood.school.ui.feature.dashboard

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.data.remote.dto.AnalyticsDashboardDto
import com.greenwood.school.data.remote.dto.CalendarEventDto
import com.greenwood.school.data.remote.dto.LibraryDashboardDto
import com.greenwood.school.data.remote.dto.NoticeDto
import com.greenwood.school.data.remote.dto.PayrollDashboardDto
import com.greenwood.school.data.remote.dto.StudentAttendanceSummaryDto
import com.greenwood.school.ui.components.DashboardSkeleton
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.OfflineBanner
import com.greenwood.school.ui.components.SectionCard
import com.greenwood.school.ui.components.StatCard
import com.greenwood.school.ui.theme.NumericType
import com.greenwood.school.ui.theme.SchoolTheme
import com.greenwood.school.ui.theme.BrandAmber
import com.greenwood.school.ui.theme.BrandIndigo
import com.greenwood.school.ui.theme.BrandIndigoLight
import com.greenwood.school.ui.theme.Sizing
import com.greenwood.school.ui.theme.StatusPalette
import com.greenwood.school.ui.theme.spacing
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * The mobile dashboard. Stat tiles two-per-row instead of the web's six-across
 * strip, then the same "recent notices" and "upcoming events" widgets the web
 * dashboard carries.
 *
 * Charts from the web version (attendance-by-class bar, fee-by-month line) are
 * represented here as the aggregate figure plus a link into Reports — a 12-series
 * chart on a 5-inch screen is noise, and Reports is where the breakdown belongs.
 *
 * The greeting lives in a [LargeTopAppBar] that collapses as the user scrolls, so
 * it reads as a proper welcome on arrival but surrenders its height to the data
 * once they start working. Every dimension below comes from the spacing and type
 * tokens in `ui/theme` — there are no literal font sizes or ad-hoc paddings here.
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

    DashboardScaffold(
        state = state,
        isOnline = isOnline,
        onOpenNotices = onOpenNotices,
        onOpenCalendar = onOpenCalendar,
        onOpenSearch = onOpenSearch,
        onOpenFees = onOpenFees,
        onOpenAttendance = onOpenAttendance,
    )
}

/**
 * The dashboard with no ViewModel attached.
 *
 * Split out from [DashboardScreen] so the layout can be rendered from a plain
 * [DashboardUiState] — which is what makes the `@Preview`s at the bottom of this
 * file possible. Hilt cannot construct a ViewModel inside a preview, so a screen
 * that reads `hiltViewModel()` directly can never be previewed; this is the
 * standard stateful/stateless split that avoids that.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardScaffold(
    state: DashboardUiState,
    isOnline: Boolean,
    onOpenNotices: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFees: () -> Unit,
    onOpenAttendance: () -> Unit,
) {
    // exitUntilCollapsed keeps the bar out of the way once collapsed instead of
    // springing back on the first upward flick, which is the right behaviour for a
    // long scrolling dashboard.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = greeting(state.greetingName),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    }
                    // The badge is the only place the notice count is surfaced once
                    // the section below has scrolled away.
                    BadgedBox(
                        badge = {
                            if (state.notices.isNotEmpty()) {
                                androidx.compose.material3.Badge {
                                    Text(state.notices.size.toString())
                                }
                            }
                        },
                    ) {
                        IconButton(onClick = onOpenNotices) {
                            Icon(Icons.Outlined.Campaign, contentDescription = "Notices")
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    // Tonal shift on scroll is what separates the bar from the
                    // content passing underneath it — no shadow needed.
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OfflineBanner(visible = !isOnline)

            Crossfade(
                targetState = state.isLoading,
                animationSpec = tween(durationMillis = 250),
                label = "dashboard-load",
            ) { loading ->
                if (loading) {
                    DashboardSkeleton(
                        modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
                        statTiles = if (state.role in Role.SELF_SERVICE) 2 else 4,
                    )
                } else {
                    DashboardContent(
                        state = state,
                        listState = listState,
                        onOpenNotices = onOpenNotices,
                        onOpenCalendar = onOpenCalendar,
                        onOpenFees = onOpenFees,
                        onOpenAttendance = onOpenAttendance,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onOpenNotices: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenFees: () -> Unit,
    onOpenAttendance: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val tiles = rememberTiles(state, onOpenFees, onOpenAttendance)
    val isBlank = state.notices.isEmpty() && state.upcomingEvents.isEmpty() && tiles.isEmpty()

    if (isBlank) {
        EmptyView(
            title = "Nothing to show yet",
            message = "Your dashboard fills up as the school publishes notices, " +
                "events and records for your account.",
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = spacing.gutter,
            end = spacing.gutter,
            top = spacing.sm,
            // Runway so the last card clears the bottom navigation bar.
            bottom = spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "role") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoleChip(state.role)
            }
        }
        // Quick-action horizontal strip (role-filtered)
        item(key = "quick-actions") {
            QuickActionsStrip(
                onOpenAttendance = onOpenAttendance,
                onOpenFees = onOpenFees,
                onOpenCalendar = onOpenCalendar,
                onOpenNotices = onOpenNotices,
            )
        }

        if (tiles.isNotEmpty()) {
            item(key = "stats") {
                StatGrid(tiles = tiles)
            }
        }

        if (state.notices.isNotEmpty()) {
            item(key = "notices") {
                SectionCard(
                    title = "Recent notices",
                    action = { ViewAllButton(onClick = onOpenNotices) },
                ) {
                    state.notices.forEachIndexed { index, notice ->
                        NoticeRow(
                            title = notice.title,
                            body = notice.description,
                            timestamp = Formatters.dateTime(notice.publishedAt),
                        )
                        if (index != state.notices.lastIndex) {
                            Spacer(Modifier.height(spacing.md))
                        }
                    }
                }
            }
        }

        if (state.upcomingEvents.isNotEmpty()) {
            item(key = "events") {
                SectionCard(
                    title = "Upcoming events",
                    action = { ViewAllButton(onClick = onOpenCalendar) },
                ) {
                    state.upcomingEvents.forEachIndexed { index, event ->
                        EventRow(
                            title = event.title,
                            type = Formatters.humanizeEnum(event.eventType),
                            rawDate = event.eventDate,
                        )
                        if (index != state.upcomingEvents.lastIndex) {
                            Spacer(Modifier.height(spacing.md))
                        }
                    }
                }
            }
        }
    }
}

/** Two tiles per row — the widest layout that keeps a rupee figure legible on a phone. */
@Composable
private fun StatGrid(tiles: List<Tile>) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                row.forEach { tile ->
                    StatCard(
                        label = tile.label,
                        value = tile.value,
                        icon = tile.icon,
                        accent = tile.accent,
                        caption = tile.caption,
                        onClick = tile.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keeps a lone trailing tile at half width instead of stretching it.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * The user's role, as a chip rather than the grey caption it used to be. It is the
 * one piece of context that explains why this dashboard looks the way it does, so
 * it earns a container.
 */
@Composable
private fun RoleChip(role: Role) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(role.label, style = MaterialTheme.typography.labelMedium) },
        shape = MaterialTheme.shapes.small,
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        border = null,
    )
}

/** Text button + trailing arrow, used as the `action` slot on every section header. */
@Composable
private fun ViewAllButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(
            horizontal = MaterialTheme.spacing.sm,
            vertical = MaterialTheme.spacing.xs,
        ),
    ) {
        Text("View all", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(MaterialTheme.spacing.xs))
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(Sizing.iconSm),
        )
    }
}

@Composable
private fun NoticeRow(title: String, body: String?, timestamp: String?) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Color accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BrandIndigo, BrandAmber.copy(alpha = 0.6f))
                    )
                )
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!body.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!timestamp.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/**
 * Event row led by a calendar-tile date. Reading "12 SEP" off a block is faster
 * than parsing a formatted date string at the end of the row, which is where the
 * previous layout put it.
 */
@Composable
private fun EventRow(title: String, type: String, rawDate: String?) {
    val parsed = Formatters.parseDate(rawDate)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(Sizing.avatarLg)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (parsed == null) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Sizing.iconMd),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = parsed.dayOfMonth.toString(),
                        style = NumericType.statValue,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = parsed.month
                            .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                            .uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.width(MaterialTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(MaterialTheme.spacing.xs))
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                shape = MaterialTheme.shapes.small,
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = null,
            )
        }
    }
}

/**
 * Builds the role-appropriate tile set.
 *
 * Extracted out of the layout so the composable above describes *arrangement* and
 * this describes *content* — the two used to be interleaved in one 120-line function.
 */
@Composable
private fun rememberTiles(
    state: DashboardUiState,
    onOpenFees: () -> Unit,
    onOpenAttendance: () -> Unit,
): List<Tile> {
    val brand = MaterialTheme.colorScheme.primary
    return buildList {
        state.analytics?.let { analytics ->
            add(Tile("Active students", Formatters.number(analytics.totalActiveStudents), Icons.Outlined.School, brand))
            add(Tile("Active teachers", Formatters.number(analytics.totalActiveTeachers), Icons.Outlined.Badge, brand))
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
            add(Tile("Books issued", Formatters.number(library.issuedCount), Icons.Outlined.MenuBook, brand))
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
            val healthy = summary.percentage >= MIN_HEALTHY_ATTENDANCE
            add(
                Tile(
                    label = "My attendance",
                    value = Formatters.percent(summary.percentage),
                    icon = Icons.Outlined.EventAvailable,
                    accent = if (healthy) StatusPalette.positive else StatusPalette.caution,
                    // The caption is where a bare percentage becomes actionable.
                    caption = if (healthy) "On track" else "Below the 75% requirement",
                    onClick = onOpenAttendance,
                ),
            )
        }

        if (state.role in Role.SELF_SERVICE) {
            val owes = state.myOutstandingFees > 0
            add(
                Tile(
                    label = "My outstanding fees",
                    value = Formatters.currency(state.myOutstandingFees),
                    icon = Icons.Outlined.Paid,
                    accent = if (owes) StatusPalette.caution else StatusPalette.positive,
                    caption = if (owes) "Payment due" else "All settled",
                    onClick = onOpenFees,
                ),
            )
        }
    }
}

private data class Tile(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val accent: Color,
    val caption: String? = null,
    val onClick: (() -> Unit)? = null,
)

private fun greeting(firstName: String): String =
    if (firstName.isBlank()) "Welcome back" else "Hello, $firstName"

private const val MIN_HEALTHY_ATTENDANCE = 75.0

@Composable
private fun QuickActionsStrip(
    onOpenAttendance: () -> Unit,
    onOpenFees: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenNotices: () -> Unit,
) {
    val actions = listOf(
        Triple("Attendance", Icons.Outlined.EventNote, onOpenAttendance),
        Triple("Fees", Icons.Outlined.AttachMoney, onOpenFees),
        Triple("Calendar", Icons.Outlined.CalendarToday, onOpenCalendar),
        Triple("Notices", Icons.Outlined.Campaign, onOpenNotices),
        Triple("Students", Icons.Outlined.Groups, null as (() -> Unit)?),
        Triple("Library", Icons.Outlined.LibraryBooks, null as (() -> Unit)?),
        Triple("Timetable", Icons.Outlined.Schedule, null as (() -> Unit)?),
        Triple("Profile", Icons.Outlined.Person, null as (() -> Unit)?),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.forEach { (label, icon, onClick) ->
            QuickActionTile(label = label, icon = icon, onClick = onClick ?: {})
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f),
        label = "tile-scale",
    )
    Column(
        modifier = Modifier
            .scale(scale)
            .size(width = 72.dp, height = 80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(BrandIndigo, BrandIndigoLight),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Previews                                                                   */
/*                                                                            */
/* Android Studio renders these in the split pane with no emulator and no     */
/* device — open this file and click "Split" or "Design" (top-right of the    */
/* editor). Build > Refresh preview if the pane looks stale after an edit.    */
/*                                                                            */
/* Sample dates are fixed rather than derived from `now()` so a preview looks */
/* identical every time it is rendered, and screenshots taken from it stay    */
/* comparable across days.                                                    */
/* ------------------------------------------------------------------------- */

private object Sample {

    val notices = listOf(
        NoticeDto(
            id = 1,
            title = "Parent-teacher meeting moved to Saturday",
            description = "The PTM originally scheduled for Friday has been moved to " +
                "Saturday 10:00 AM in the main auditorium.",
            publishedAt = "2026-08-20T09:30:00",
        ),
        NoticeDto(
            id = 2,
            title = "Half-day on account of Founder's Day",
            description = "School closes at 12:30 PM. Bus routes will run on the " +
                "revised half-day timetable.",
            publishedAt = "2026-08-18T16:05:00",
        ),
        NoticeDto(
            id = 3,
            title = "Library books due before term break",
            description = "All issued titles must be returned by the 28th to avoid " +
                "overdue charges.",
            publishedAt = "2026-08-17T11:00:00",
        ),
    )

    val events = listOf(
        CalendarEventDto(id = 1, title = "Unit Test II — Mathematics", eventDate = "2026-09-02", eventType = "EXAM"),
        CalendarEventDto(id = 2, title = "Inter-house Athletics Meet", eventDate = "2026-09-12", eventType = "SPORTS"),
        CalendarEventDto(id = 3, title = "Teachers' Day Assembly", eventDate = "2026-09-05", eventType = "HOLIDAY"),
    )

    /** A principal: school-wide rollups, payroll and library all present. */
    val management = DashboardUiState(
        role = Role.PRINCIPAL,
        greetingName = "Pushkar",
        isLoading = false,
        analytics = AnalyticsDashboardDto(
            totalActiveStudents = 1284,
            totalActiveTeachers = 76,
            totalFeeCollected = 8_452_000.0,
            totalFeeOutstanding = 1_130_500.0,
            averageAttendancePercentage = 92.4,
        ),
        payroll = PayrollDashboardDto(totalPaid = 3_900_000.0, totalPending = 615_000.0, employeeCount = 76),
        library = LibraryDashboardDto(totalBooks = 4200, issuedCount = 318, overdueCount = 12),
        notices = notices,
        upcomingEvents = events,
    )

    /** A student: two self-service tiles, one of them in a warning state. */
    val student = DashboardUiState(
        role = Role.STUDENT,
        greetingName = "Aarav",
        isLoading = false,
        myAttendance = StudentAttendanceSummaryDto(
            presentDays = 118,
            absentDays = 21,
            totalMarkedDays = 139,
            percentage = 68.5,
        ),
        myOutstandingFees = 24_500.0,
        notices = notices.take(2),
        upcomingEvents = events.take(2),
    )
}

@Composable
private fun PreviewHost(state: DashboardUiState, isOnline: Boolean = true) {
    SchoolTheme {
        DashboardScaffold(
            state = state,
            isOnline = isOnline,
            onOpenNotices = {},
            onOpenCalendar = {},
            onOpenSearch = {},
            onOpenFees = {},
            onOpenAttendance = {},
        )
    }
}

/** The busiest case: every management tile populated, both sections full. */
@PreviewLightDark
@Composable
private fun DashboardManagementPreview() = PreviewHost(Sample.management)

/** Self-service layout — fewer tiles, and the attendance tile in its warning state. */
@PreviewLightDark
@Composable
private fun DashboardStudentPreview() = PreviewHost(Sample.student)

/** First load. Shows the shimmer skeleton rather than a bare spinner. */
@Preview(name = "Loading", showBackground = true)
@Composable
private fun DashboardLoadingPreview() =
    PreviewHost(DashboardUiState(role = Role.PRINCIPAL, greetingName = "Pushkar", isLoading = true))

/** A brand-new account with nothing published to it yet. */
@Preview(name = "Empty", showBackground = true)
@Composable
private fun DashboardEmptyPreview() =
    PreviewHost(DashboardUiState(role = Role.TEACHER, greetingName = "Meera", isLoading = false))

/** Offline banner pinned above content that is still readable from cache. */
@Preview(name = "Offline", showBackground = true)
@Composable
private fun DashboardOfflinePreview() = PreviewHost(Sample.management, isOnline = false)

/** Tablet width — confirms the two-up stat grid still breathes rather than stretching. */
@Preview(name = "Tablet", showBackground = true, widthDp = 840, heightDp = 900)
@Composable
private fun DashboardTabletPreview() = PreviewHost(Sample.management)

/** Largest accessibility font size — the usual place a tile's label starts clipping. */
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun DashboardLargeFontPreview() = PreviewHost(Sample.student)

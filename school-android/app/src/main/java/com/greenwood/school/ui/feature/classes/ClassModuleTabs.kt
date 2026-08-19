package com.greenwood.school.ui.feature.classes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.data.remote.dto.ClassOfficialDto
import com.greenwood.school.data.remote.dto.ClassOverviewDto
import com.greenwood.school.data.remote.dto.TimetableSlotDto
import com.greenwood.school.ui.components.DetailRow
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.SectionCard
import com.greenwood.school.ui.components.StatCard

/* ------------------------------------------------------------------------- */
/* Class module tabs: overview, posts and timetable.                          */
/*                                                                            */
/* Read-only on the phone. Appointing a post-holder and editing the weekly    */
/* grid stay on the web app, where the dropdowns and clash handling live;     */
/* what a phone is actually wanted for here is looking the answers up.        */
/* ------------------------------------------------------------------------- */

/**
 * Strength against capacity, the cross-module figures, and the setup gaps an
 * administrator can act on.
 *
 * A missing figure renders as a placeholder rather than 0: a class with no
 * marked attendance or no graded exam has nothing to report, and a zero would
 * read as a bad result instead of an absent one.
 */
@Composable
fun ClassOverviewTab(overview: ClassOverviewDto?) {
    if (overview == null) {
        EmptyView(title = "No overview", message = "This class could not be summarised.")
        return
    }

    val stats = overview.stats
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label = "Students",
                    value = overview.totalStudents.toString(),
                    icon = Icons.Outlined.Groups,
                    modifier = Modifier.weight(1f),
                    caption = overview.totalCapacity?.let { "of $it seats" },
                )
                StatCard(
                    label = "Attendance",
                    value = stats?.attendancePercentage?.let { "$it%" } ?: Formatters.PLACEHOLDER,
                    icon = Icons.Outlined.CalendarMonth,
                    modifier = Modifier.weight(1f),
                    caption = stats?.attendanceMarkedDays?.let { "$it marked days" },
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label = "Sections",
                    value = "${overview.totalSections} / ${overview.totalSubjects}",
                    icon = Icons.Outlined.Class,
                    modifier = Modifier.weight(1f),
                    caption = "sections / subjects",
                )
                StatCard(
                    label = "Fee defaulters",
                    value = stats?.feeDefaulterCount?.toString() ?: Formatters.PLACEHOLDER,
                    icon = Icons.Outlined.Payments,
                    modifier = Modifier.weight(1f),
                    caption = stats?.feeOutstandingAmount?.let { "outstanding $it" },
                )
            }
        }

        item {
            SectionCard(title = "Strength") {
                DetailRow(
                    label = "Occupancy",
                    value = overview.occupancyPercentage?.let { "$it%" }
                        ?: "No section declares a capacity",
                )
                overview.genderSplit.forEach { (gender, count) ->
                    DetailRow(label = Formatters.humanizeEnum(gender), value = count.toString())
                }
                DetailRow(
                    label = "Average marks",
                    value = stats?.averageMarksPercentage?.let { "$it%" } ?: Formatters.PLACEHOLDER,
                )
            }
        }

        item {
            SectionCard(title = "Setup checks") {
                if (overview.warnings.isEmpty()) {
                    Text(
                        "Nothing outstanding - every section has a class teacher " +
                            "and every subject a teacher.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    overview.warnings.forEach { warning ->
                        DetailRow(
                            label = if (warning.isWarning) "Warning" else "Note",
                            value = warning.message,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Current post-holders, followed by the tenures that have ended.
 *
 * The history endpoint returns live rows too; they are filtered out here because
 * repeating a sitting holder under "Previously" reads as though the post had
 * already changed hands.
 */
@Composable
fun ClassOfficialsTab(
    current: List<ClassOfficialDto>,
    history: List<ClassOfficialDto>,
) {
    if (current.isEmpty() && history.isEmpty()) {
        EmptyView(
            title = "No posts filled",
            message = "Head boy, head girl and other posts appear here once appointed.",
        )
        return
    }

    val past = history.filter { !it.current }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(current.size, key = { "current-${current[it].id}" }) { index ->
            val official = current[index]
            EntityRowCard(
                title = official.displayName,
                subtitle = official.roleLabel,
                metadata = listOfNotNull(
                    official.rollNumber?.let { "Roll $it" },
                    official.fromDate?.let { "Since ${Formatters.date(it)}" },
                ).joinToString(" · ").ifBlank { null },
                leadingInitials = Formatters.initials(official.displayName),
            )
        }

        if (past.isNotEmpty()) {
            item(key = "past-header") {
                Text(
                    "Previously",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(past.size, key = { "past-${past[it].id}" }) { index ->
                val official = past[index]
                EntityRowCard(
                    title = official.displayName,
                    subtitle = official.roleLabel,
                    metadata = "${Formatters.date(official.fromDate)} - " +
                        Formatters.date(official.toDate),
                    leadingInitials = Formatters.initials(official.displayName),
                )
            }
        }
    }
}

/**
 * The week, grouped by day and ordered by period.
 *
 * A clash warning is shown on the slot carrying it rather than hidden: the
 * backend reports clashes instead of refusing them, because a timetable built
 * one section at a time is legitimately in conflict until the rest are filled in.
 */
@Composable
fun ClassTimetableTab(slots: List<TimetableSlotDto>) {
    if (slots.isEmpty()) {
        EmptyView(
            title = "No timetable yet",
            message = "Periods appear here once the weekly grid is filled in.",
        )
        return
    }

    val dayOrder = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY",
    )
    val byDay = slots
        .groupBy { it.dayOfWeek }
        .toList()
        // An unrecognised day sorts last rather than throwing off the whole week.
        .sortedBy { (day, _) -> dayOrder.indexOf(day).takeIf { it >= 0 } ?: dayOrder.size }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        byDay.forEach { (day, daySlots) ->
            item(key = "day-$day") {
                Text(
                    Formatters.humanizeEnum(day),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            val ordered = daySlots.sortedWith(
                compareBy({ it.periodNumber }, { it.className.orEmpty() }),
            )
            items(ordered.size, key = { "slot-$day-${ordered[it].id ?: it}" }) { index ->
                val slot = ordered[index]
                EntityRowCard(
                    title = slot.title,
                    // The section is no longer named: the school runs one per class, so
                    // "Section A" repeated down the list said nothing. The class does
                    // carry information — a teacher's own week spans several.
                    subtitle = listOfNotNull(
                        slot.className,
                        slot.teacherName,
                    ).joinToString(" · ").ifBlank { null },
                    metadata = listOfNotNull(
                        slot.timeRange,
                        slot.roomNumber?.let { "Room $it" },
                        slot.clashWarning,
                    ).joinToString(" · ").ifBlank { null },
                    leadingInitials = slot.periodNumber.toString(),
                )
            }
        }
    }
}

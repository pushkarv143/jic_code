package com.greenwood.school.ui.feature.classes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.data.remote.dto.ClassOfficialDto
import com.greenwood.school.data.remote.dto.ClassOverviewDto
import com.greenwood.school.data.remote.dto.StudentDto
import com.greenwood.school.data.remote.dto.TimetableSlotDto
import com.greenwood.school.ui.components.DetailRow
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.SectionCard
import com.greenwood.school.ui.components.StatCard

/* ------------------------------------------------------------------------- */
/* Class module tabs: overview, posts and timetable.                          */
/*                                                                            */
/* The overview and the weekly grid are read-only - both are summaries, and    */
/* the grid is edited a period at a time from the Teachers tab, where the      */
/* teacher a period needs is already on screen. Posts are appointed and ended  */
/* here, gated on CLASS_MANAGE, which is what the endpoints behind them ask    */
/* for.                                                                       */
/* ------------------------------------------------------------------------- */

/** The posts a class can fill, and who is eligible for each. Matches the web. */
private val OFFICIAL_ROLES = listOf(
    Triple("HEAD_BOY", "Head Boy", "MALE"),
    Triple("HEAD_GIRL", "Head Girl", "FEMALE"),
    Triple("MONITOR", "Monitor", null),
    Triple("SPORTS_CAPTAIN", "Sports Captain", null),
    Triple("CULTURAL_SECRETARY", "Cultural Secretary", null),
)

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
    /**
     * CLASS_MANAGE. Appointing school-wide is the office's job; a class teacher
     * appoints within their own section on the My Class screen instead, which is
     * homeroom-scoped server-side.
     */
    canManage: Boolean = false,
    /** This class's active students - the candidates for a post. */
    students: List<StudentDto> = emptyList(),
    onAppoint: (Long, String) -> Unit = { _, _ -> },
    onEnd: (Long) -> Unit = {},
) {
    var appointing by remember { mutableStateOf(false) }

    if (appointing) {
        AppointDialog(
            students = students,
            onDismiss = { appointing = false },
            onConfirm = { studentId, role ->
                appointing = false
                onAppoint(studentId, role)
            },
        )
    }

    // An empty class still needs the button: the first post is appointed from here.
    if (current.isEmpty() && history.isEmpty()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EmptyView(
                title = "No posts filled",
                message = "Head boy, head girl and other posts appear here once appointed.",
            )
            if (canManage) {
                Button(onClick = { appointing = true }) { Text("Appoint") }
            }
        }
        return
    }

    val past = history.filter { !it.current }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (canManage) {
            item(key = "appoint") {
                // Appointing over a sitting holder is a succession, not a
                // replacement: the server ends the current tenure and opens a new
                // one, so the history below keeps both.
                Button(onClick = { appointing = true }) { Text("Appoint") }
            }
        }

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
                // Ending a tenure closes it rather than deleting the record, so the
                // history below keeps every holder the class has had.
                trailing = if (canManage) {
                    {
                        androidx.compose.material3.TextButton(onClick = { onEnd(official.id) }) {
                            Text("End term")
                        }
                    }
                } else {
                    null
                },
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
 * Picks a post and a student for it.
 *
 * The eligible list narrows with the post - only boys can be head boy - and the
 * choice is cleared when the post changes, because a student picked under the
 * previous one may no longer qualify. The server enforces the same rule; this
 * just stops the app offering a save it would refuse.
 */
@Composable
private fun AppointDialog(
    students: List<StudentDto>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit,
) {
    var role by remember { mutableStateOf(OFFICIAL_ROLES.first()) }
    var studentId by remember { mutableStateOf<Long?>(null) }
    val eligible = role.third?.let { required -> students.filter { it.gender == required } }
        ?: students

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appoint a post-holder") },
        text = {
            Column {
                Text("Post", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(OFFICIAL_ROLES.size, key = { OFFICIAL_ROLES[it].first }) { i ->
                        val option = OFFICIAL_ROLES[i]
                        FilterChip(
                            selected = option.first == role.first,
                            onClick = {
                                role = option
                                studentId = null
                            },
                            label = { Text(option.second) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Student", style = MaterialTheme.typography.labelLarge)
                if (eligible.isEmpty()) {
                    Text(
                        role.third?.let { "No ${it.lowercase()} students in this class." }
                            ?: "No active students in this class.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(eligible.size, key = { eligible[it].id }) { i ->
                            val student = eligible[i]
                            val name = listOfNotNull(student.firstName, student.lastName)
                                .joinToString(" ")
                                .ifBlank { student.admissionNumber }
                            FilterChip(
                                selected = studentId == student.id,
                                onClick = { studentId = student.id },
                                label = {
                                    Text(
                                        student.rollNumber?.let { "$name ($it)" } ?: name,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = studentId != null,
                onClick = { studentId?.let { onConfirm(it, role.first) } },
            ) { Text("Appoint") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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

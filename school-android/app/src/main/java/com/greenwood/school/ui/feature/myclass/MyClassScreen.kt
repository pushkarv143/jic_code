package com.greenwood.school.ui.feature.myclass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenwood.school.data.remote.dto.ClassOfficialDto
import com.greenwood.school.data.remote.dto.StudentDto
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.SearchField

/** The posts a class can fill, in the order a school lists them. */
private val POSTS = listOf(
    "HEAD_BOY" to "Head Boy",
    "HEAD_GIRL" to "Head Girl",
    "MONITOR" to "Monitor",
    "SPORTS_CAPTAIN" to "Sports Captain",
    "CULTURAL_SECRETARY" to "Cultural Secretary",
)

/**
 * The class teacher's own section — the phone counterpart of the web app's My Class.
 *
 * <p>Exists because the whole-school Classes screen answers a different question:
 * that one is a directory a teacher may read, this is the single section they are
 * accountable for and may actually change.
 *
 * <p>There is deliberately **no "add student"**. Admitting a pupil creates a login,
 * an admission number and a guardian record and decides which class they join — the
 * office's job, and the API has no route for a class teacher to do it. A class
 * teacher edits the records of students already on their roster.
 */
@Composable
fun MyClassScreen(
    onBack: () -> Unit,
    onOpenStudent: (Long) -> Unit,
    viewModel: MyClassViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = state.homeroom?.className?.let { "$it — My Class" } ?: "My Class",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                /*
                 * Reached by someone who holds MY_CLASS_VIEW but no assignment — 27 of
                 * the 44 CLASS_TEACHER role holders on the seeded database. The menu
                 * entry is hidden for them, but a deep link still lands here, so it
                 * explains itself rather than showing an empty list or a bare 403.
                 */
                !state.hasHomeroom -> EmptyView(
                    title = "You have no class",
                    message = "This screen shows the roster for the section you are class " +
                        "teacher of. Ask the office to assign you one and it will appear here.",
                )

                state.isLoading && state.students.isEmpty() -> FullScreenLoader()

                state.error != null && state.students.isEmpty() ->
                    ErrorView(error = state.error!!, onRetry = viewModel::load)

                else -> {
                    TabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Students") })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Class Posts") })
                    }
                    Spacer(Modifier.height(8.dp))
                    if (tab == 0) {
                        RosterTab(
                            students = state.students,
                            total = state.total,
                            search = state.search,
                            onSearch = viewModel::search,
                            onOpenStudent = onOpenStudent,
                        )
                    } else {
                        PostsTab(
                            officials = state.officials,
                            students = state.students,
                            canManage = state.canManageOfficials,
                            onAppoint = viewModel::appointOfficial,
                            onEnd = viewModel::endOfficial,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RosterTab(
    students: List<StudentDto>,
    total: Long,
    search: String,
    onSearch: (String) -> Unit,
    onOpenStudent: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SearchField(value = search, onValueChange = onSearch, placeholder = "Search students")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$total student${if (total == 1L) "" else "s"} on your roster",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        if (students.isEmpty()) {
            EmptyView(
                title = "No students yet",
                message = "Students are enrolled by the office. Once they are, they appear here.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(students, key = { it.id }) { student ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Roll number is read-only everywhere: it is the
                                // student's position in the class, assigned by the
                                // server and unique per class.
                                Text(
                                    text = student.rollNumber?.let { "$it." } ?: "-",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Spacer(Modifier.height(0.dp))
                                Text(
                                    text = "  ${student.displayName}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                            Text(
                                text = student.admissionNumber,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(onClick = { onOpenStudent(student.id) }) { Text("Open") }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Class posts for the caller's own section.
 *
 * <p>Reading is open to anyone who can reach this screen — who holds a post is
 * roster information. Changing is MY_CLASS_OFFICIALS_MANAGE, a separate grant from
 * the roster one, so a school can let the class teacher keep records while reserving
 * "who is head boy" for the principal, or the reverse.
 */
@Composable
private fun PostsTab(
    officials: List<ClassOfficialDto>,
    students: List<StudentDto>,
    canManage: Boolean,
    onAppoint: (Long, String) -> Unit,
    onEnd: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            text = if (canManage) {
                "Appointing a student ends the sitting holder's tenure — the history is kept."
            } else {
                "You can see who holds each post. Appointing is handled by the office."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(POSTS, key = { it.first }) { (wire, label) ->
                val holder = officials.firstOrNull { it.role == wire && it.current }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(label, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = holder?.studentName ?: "Vacant",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (canManage) {
                            if (holder != null) {
                                TextButton(onClick = { onEnd(holder.id) }) { Text("End appointment") }
                            } else if (students.isNotEmpty()) {
                                // One chip per candidate rather than a dropdown: a
                                // primary class is ~30 pupils and a phone list is
                                // easier to scan than a spinner.
                                Row(Modifier.fillMaxWidth()) {
                                    students.take(APPOINT_CHOICES).forEach { student ->
                                        AssistChip(
                                            onClick = { onAppoint(student.id, wire) },
                                            label = { Text(student.displayName) },
                                            modifier = Modifier.padding(end = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * How many candidates the appoint row offers before it would wrap off-screen.
 *
 * The server rejects an ineligible pick — a head boy must be male, a student holds
 * one post at a time — so this is a convenience cap, not the rule.
 */
private const val APPOINT_CHOICES = 3

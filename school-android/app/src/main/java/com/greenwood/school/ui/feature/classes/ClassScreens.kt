package com.greenwood.school.ui.feature.classes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.isManagement
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.core.session.AccessStore
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.dto.ClassOfficialDto
import com.greenwood.school.data.remote.dto.ClassOfficialRequestDto
import com.greenwood.school.data.remote.dto.ClassOverviewDto
import com.greenwood.school.data.remote.dto.ClassSubjectTeacherDto
import com.greenwood.school.data.remote.dto.ClassSubjectTeacherRequestDto
import com.greenwood.school.data.remote.dto.SaveTimetableRequestDto
import com.greenwood.school.data.remote.dto.SchoolClassDto
import com.greenwood.school.data.remote.dto.SchoolClassRequestDto
import com.greenwood.school.data.remote.dto.SectionDto
import com.greenwood.school.data.remote.dto.SectionRequestDto
import com.greenwood.school.data.remote.dto.StudentDto
import com.greenwood.school.data.remote.dto.SubjectDto
import com.greenwood.school.data.remote.dto.SubjectRequestDto
import com.greenwood.school.data.remote.dto.TeacherDto
import com.greenwood.school.data.remote.dto.TimetableSlotDto
import com.greenwood.school.data.remote.dto.TimetableSlotRequestDto
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.domain.repository.PeopleRepository
import com.greenwood.school.domain.repository.StudentRepository
import com.greenwood.school.navigation.Routes
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.common.UiMessage
import com.greenwood.school.ui.components.SimpleListScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ------------------------------------------------------------------------- */
/* Class list                                                                 */
/* ------------------------------------------------------------------------- */

@Composable
fun ClassListScreen(
    onOpenClass: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ClassListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    SimpleListScaffold(
        title = "Classes & Subjects",
        subtitle = state.currentYearName,
        items = state.classes,
        isLoading = state.isLoading,
        error = state.error,
        onRefresh = viewModel::load,
        onBack = onBack,
        message = state.message,
        onMessageShown = viewModel::consumeMessage,
        emptyTitle = "No classes yet",
        emptyMessage = "Classes appear here once the academic year is set up.",
        // No add button without CLASS_MANAGE, and none until the current academic
        // year is known either: a class must belong to one, and the year is what the
        // list is already scoped by rather than something to ask for again.
        onAdd = if (state.canManage && state.currentYearId != null) {
            { showAdd = true }
        } else {
            null
        },
        key = { it.id },
    ) { schoolClass ->
        EntityRowCard(
            title = schoolClass.className,
            // Section count dropped: it now reads "1 section" on every row, which is
            // a fact about the school rather than about this class.
            subtitle = schoolClass.studentCount
                ?.let { "$it student${if (it == 1) "" else "s"}" },
            metadata = schoolClass.academicYearName,
            leadingInitials = schoolClass.className.filter { it.isDigit() }.ifBlank { "C" },
            showChevron = true,
            onClick = { onOpenClass(schoolClass.id) },
        )
    }

    if (showAdd) {
        ClassNameDialog(
            title = "Add class",
            initial = "",
            confirmLabel = "Add",
            onDismiss = { showAdd = false },
            onConfirm = { name ->
                showAdd = false
                viewModel.createClass(name)
            },
        )
    }
}

/**
 * Asks for a class name — used both to add one and to rename one.
 *
 * The academic year is not asked for: it comes from the year the list is scoped to,
 * which is the current one. Adding a class to a past year is a correction rather
 * than daily work, and the web client is the place for it.
 */
@Composable
private fun ClassNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Class name") },
                singleLine = true,
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = name.isNotBlank() && name.trim() != initial,
                onClick = { onConfirm(name.trim()) },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@HiltViewModel
class ClassListViewModel @Inject constructor(
    private val academicRepository: AcademicRepository,
    accessStore: AccessStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        // CLASS_MANAGE, the same grant ClassController requires to accept the POST.
        ClassListUiState(canManage = accessStore.can("CLASS_MANAGE")),
    )
    val state: StateFlow<ClassListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // Scope to the current academic year, the same default the web page uses.
            val year = academicRepository.getCurrentAcademicYear().getOrNull()
            when (val result = academicRepository.getClasses(academicYearId = year?.id, forceRefresh = true)) {
                // copy(), so the add button survives the reload that follows a save.
                is ApiResult.Success -> _state.update {
                    it.copy(
                        classes = result.data,
                        currentYearName = year?.yearName,
                        currentYearId = year?.id,
                        isLoading = false,
                        error = null,
                    )
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun createClass(className: String) {
        val yearId = _state.value.currentYearId ?: return
        viewModelScope.launch {
            val result = academicRepository.createClass(
                SchoolClassRequestDto(className = className, academicYearId = yearId),
            )
            when (result) {
                is ApiResult.Success -> {
                    _state.update { it.copy(message = UiMessage.success("\"$className\" added.")) }
                    load()
                }
                // A duplicate name is the common refusal here, and the server names it.
                is ApiResult.Failure ->
                    _state.update { it.copy(message = UiMessage.error(result.error)) }
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}

data class ClassListUiState(
    val classes: List<SchoolClassDto> = emptyList(),
    val currentYearName: String? = null,
    /** A new class needs a year to belong to, so the add button waits for it. */
    val currentYearId: Long? = null,
    /** CLASS_MANAGE. */
    val canManage: Boolean = false,
    val message: UiMessage? = null,
    val isLoading: Boolean = true,
    val error: AppError? = null,
)

/* ------------------------------------------------------------------------- */
/* Class detail — sections / subjects / teacher mapping                       */
/* ------------------------------------------------------------------------- */

@Composable
fun ClassDetailScreen(
    onBack: () -> Unit,
    viewModel: ClassDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    // "Sections" is "Class Setup" now: one section per class, so the tab is that
    // section's room, capacity and class teacher rather than a list to browse.
    // Timetable is management-only, matching the endpoint behind it.
    val tabs = buildList {
        add("Overview")
        add("Class Setup")
        add("Subjects")
        add("Teachers")
        add("Posts")
        if (state.isManagement) add("Timetable")
    }

    // Every write here reports through the snackbar, success or failure. The failure
    // text is the server's own — "Period 1 must be taught by the class teacher",
    // "A teacher cannot take two classes at once" — which names the rule that was
    // broken rather than restating that something went wrong.
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    androidx.compose.runtime.LaunchedEffect(state.message?.id) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = state.schoolClass?.className ?: "Class",
                subtitle = state.schoolClass?.academicYearName,
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                tabs.forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
            }

            when {
                state.isLoading -> FullScreenLoader()
                state.error != null -> ErrorView(error = state.error!!, onRetry = viewModel::load)
                // Dispatched by label rather than index: the Timetable tab is only
                // present for management, and an index-based branch would quietly
                // shift every tab along when it is absent.
                else -> when (tabs.getOrNull(tab)) {
                    "Overview" -> ClassOverviewTab(state.overview)
                    "Class Setup" -> ClassSetupTab(
                        sections = state.sections,
                        teachers = state.teachers,
                        canAssign = state.canManageSection,
                        canManageClass = state.canManageClasses,
                        className = state.schoolClass?.className,
                        onAssignClassTeacher = viewModel::assignClassTeacher,
                        onSaveDetails = viewModel::updateSectionDetails,
                        onRename = viewModel::renameClass,
                    )
                    "Subjects" -> SubjectsTab(
                        subjects = state.subjects,
                        canManage = state.canManageSubjects,
                        onAdd = viewModel::addSubject,
                        onDelete = viewModel::deleteSubject,
                    )
                    "Teachers" -> TeacherMappingTab(
                        mappings = state.mappings,
                        subjects = state.subjects,
                        teachers = state.teachers,
                        timetable = state.timetable,
                        sectionId = state.sections.firstOrNull()?.id,
                        canManageMapping = state.canManageSubjects,
                        canManageTimetable = state.canManageTimetable,
                        onAssignTeacher = viewModel::assignSubjectTeacher,
                        onApplyPeriod = viewModel::applyPeriod,
                    )
                    "Posts" -> ClassOfficialsTab(
                        current = state.officials,
                        history = state.officialHistory,
                        canManage = state.canManageClasses,
                        students = state.students,
                        onAppoint = viewModel::appointOfficial,
                        onEnd = viewModel::endOfficial,
                    )
                    "Timetable" -> ClassTimetableTab(state.timetable)
                    else -> ClassOverviewTab(state.overview)
                }
            }
        }
    }
}

/**
 * The class's room, capacity and class teacher.
 *
 * Reads as the class's own details rather than as a section: the school runs one
 * section per class, so naming it added a letter and no information. It still maps
 * over the list it is given instead of assuming a single entry — the data is what
 * decides that, not this screen.
 */
@Composable
private fun ClassSetupTab(
    sections: List<SectionDto>,
    teachers: List<TeacherDto>,
    canAssign: Boolean,
    /** CLASS_MANAGE - renaming the class itself, as opposed to its section record. */
    canManageClass: Boolean,
    className: String?,
    onAssignClassTeacher: (Long, Long) -> Unit,
    onSaveDetails: (SectionDto, String, String) -> Unit,
    onRename: (String) -> Unit,
) {
    var editing by remember { mutableStateOf<SectionDto?>(null) }
    var renaming by remember { mutableStateOf(false) }

    if (sections.isEmpty()) {
        EmptyView(
            title = "Not set up yet",
            message = "This class has no section record, so students cannot be enrolled into it.",
        )
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sections.size, key = { sections[it].id }) { index ->
            val section = sections[index]
            EntityRowCard(
                title = "Room & capacity",
                subtitle = section.classTeacherName?.let { "Class teacher: $it" }
                    ?: "No class teacher assigned",
                metadata = listOfNotNull(
                    section.roomNumber?.let { "Room $it" },
                    section.studentCount?.let { "$it students" },
                    section.capacity?.let { "Capacity $it" },
                ).joinToString(" · ").ifBlank { null },
                leadingInitials = section.studentCount?.toString() ?: "-",
                trailing = if (canAssign) {
                    {
                        androidx.compose.material3.TextButton(onClick = { editing = section }) {
                            Text("Edit")
                        }
                    }
                } else {
                    null
                },
            )
            // Assigning writes sections.class_teacher_id and needs SECTION_MANAGE.
            // Without it the name above still shows - who the class teacher is, is
            // information a teacher needs - but there is no picker, because the API
            // would refuse the save.
            if (canAssign) {
                TeacherPickerRow(
                    label = "Assign class teacher",
                    teachers = teachers,
                    onPick = { teacherId -> onAssignClassTeacher(section.id, teacherId) },
                )
            } else {
                Text(
                    text = "Only an administrator can change the class teacher.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        if (canManageClass && className != null) {
            item(key = "rename") {
                androidx.compose.material3.TextButton(onClick = { renaming = true }) {
                    Text("Rename class")
                }
            }
        }
    }

    editing?.let { section ->
        SectionDetailsDialog(
            section = section,
            onDismiss = { editing = null },
            onConfirm = { room, capacity ->
                editing = null
                onSaveDetails(section, room, capacity)
            },
        )
    }

    if (renaming && className != null) {
        ClassNameDialog(
            title = "Rename class",
            initial = className,
            confirmLabel = "Save",
            onDismiss = { renaming = false },
            onConfirm = { name ->
                renaming = false
                onRename(name)
            },
        )
    }
}

/** The room and capacity of a class's section. Both may legitimately be blank. */
@Composable
private fun SectionDetailsDialog(
    section: SectionDto,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var room by remember { mutableStateOf(section.roomNumber.orEmpty()) }
    var capacity by remember { mutableStateOf(section.capacity?.toString().orEmpty()) }
    val strength = section.studentCount
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Room & capacity") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room number") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = capacity,
                    // Digits only: a capacity is a seat count, and the server rejects
                    // anything else anyway. Filtering here saves the round trip.
                    onValueChange = { input -> capacity = input.filter { it.isDigit() }.take(4) },
                    label = { Text("Capacity") },
                    singleLine = true,
                )
                // Seating fewer than are already enrolled is allowed - the school may
                // be recording a room it has outgrown - but it is worth saying so.
                if (strength != null && (capacity.toIntOrNull() ?: Int.MAX_VALUE) < strength) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This class already has $strength students.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(room, capacity) }) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * A teacher picker as a row of chips rather than a spinner.
 *
 * The list is long, so it scrolls horizontally — but a phone list of names is still
 * quicker to scan than a dropdown, and it keeps the choice and its consequence on
 * the same screen. The server has the final say on eligibility: a teacher already
 * homeroom of another section is rejected by uq_sections_class_teacher.
 */
@Composable
private fun TeacherPickerRow(
    label: String,
    teachers: List<TeacherDto>,
    onPick: (Long) -> Unit,
) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        if (teachers.isEmpty()) {
            Text("No teachers available.", style = MaterialTheme.typography.bodySmall)
        } else {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(teachers.size, key = { teachers[it].id }) { i ->
                    val t = teachers[i]
                    androidx.compose.material3.AssistChip(
                        onClick = { onPick(t.id) },
                        label = { Text(t.displayName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectsTab(
    subjects: List<SubjectDto>,
    canManage: Boolean,
    onAdd: (String, String) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SubjectDto?>(null) }

    Column(Modifier.fillMaxSize()) {
        if (canManage) {
            androidx.compose.material3.TextButton(
                onClick = { showAdd = true },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) { Text("Add subject") }
        }

        if (subjects.isEmpty()) {
            EmptyView(
                title = "No subjects",
                message = if (canManage) {
                    "Add the subjects this class is taught."
                } else {
                    "This class has no subjects yet."
                },
            )
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(subjects.size, key = { subjects[it].id }) { index ->
                    val subject = subjects[index]
                    EntityRowCard(
                        title = subject.subjectName,
                        subtitle = subject.subjectCode,
                        metadata = if (subject.isElective) "Elective" else "Core",
                        leadingInitials = subject.subjectCode.take(2),
                        // Deleting a subject cascades to its timetable slots and
                        // mapping, so it is confirmed rather than done on one tap.
                        onClick = if (canManage) {
                            { pendingDelete = subject }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    if (showAdd) {
        SubjectAddDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, code ->
                showAdd = false
                onAdd(name, code)
            },
        )
    }

    pendingDelete?.let { subject ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${subject.subjectName}?") },
            text = {
                Text(
                    "Its timetable periods and teacher mapping go with it. " +
                        "Marks already recorded against it are kept.",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pendingDelete = null
                    onDelete(subject.id)
                }) { Text("Remove") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SubjectAddDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add subject") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject name") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    // Unique school-wide (uq_subjects_code), so a clash is rejected
                    // server-side with a message naming the conflict.
                    label = { Text("Subject code") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = name.isNotBlank() && code.isNotBlank(),
                onClick = { onConfirm(name.trim(), code.trim()) },
            ) { Text("Add") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun TeacherMappingTab(
    mappings: List<ClassSubjectTeacherDto>,
    subjects: List<SubjectDto>,
    teachers: List<TeacherDto>,
    timetable: List<TimetableSlotDto>,
    sectionId: Long?,
    canManageMapping: Boolean,
    canManageTimetable: Boolean,
    onAssignTeacher: (Long, Long, Long) -> Unit,
    onApplyPeriod: (Long, Long, Int?) -> Unit,
) {
    if (subjects.isEmpty()) {
        EmptyView(
            title = "No subjects yet",
            message = "Add a subject on the Subjects tab before assigning teachers.",
        )
        return
    }

    // One row per subject rather than per mapping, so a subject with nobody assigned
    // is visible as a gap to fill instead of being absent from the list. That was the
    // web app's fix too: "who takes maths" and "when" are one decision.
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(subjects.size, key = { subjects[it].id }) { index ->
            val subject = subjects[index]
            val mapping = mappings.firstOrNull { it.subjectId == subject.id }
            val periods = timetable.filter { it.subjectId == subject.id }.map { it.periodNumber }.distinct()

            EntityRowCard(
                title = subject.subjectName,
                subtitle = mapping?.teacherName ?: "No teacher assigned",
                metadata = when {
                    periods.isEmpty() -> "Not timetabled"
                    periods.size == 1 -> "Period ${periods.first()}, Mon-Sat"
                    else -> "Periods ${periods.sorted().joinToString(", ")}"
                },
                leadingInitials = Formatters.initials(mapping?.teacherName.orEmpty())
                    .ifBlank { subject.subjectCode.take(2) },
            )

            if (canManageMapping && sectionId != null) {
                TeacherPickerRow(
                    label = if (mapping == null) "Assign a teacher" else "Reassign",
                    teachers = teachers,
                    onPick = { teacherId -> onAssignTeacher(sectionId, subject.id, teacherId) },
                )
            }

            // A subject cannot be timetabled before someone is assigned to teach it -
            // the server refuses it - so the period picker only appears once there is
            // a teacher. Period 1 is additionally reserved for the class teacher, in a
            // subject they are mapped to; that one is enforced on save.
            if (canManageTimetable && sectionId != null && mapping != null) {
                PeriodPickerRow(
                    selected = periods.singleOrNull(),
                    onPick = { period -> onApplyPeriod(sectionId, subject.id, period) },
                )
            } else if (canManageTimetable && mapping == null) {
                Text(
                    text = "Assign a teacher first, then a period.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

/**
 * Picks the period a subject occupies, filling Monday to Saturday in one tap.
 *
 * That is the shape almost every school timetable has, so asking for six identical
 * picks was busywork. "Clear" removes the subject from the week entirely. Sunday is
 * never offered.
 */
@Composable
private fun PeriodPickerRow(selected: Int?, onPick: (Int?) -> Unit) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Text("Period (fills Mon-Sat)", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(PERIOD_COUNT, key = { it }) { i ->
                val period = i + 1
                androidx.compose.material3.FilterChip(
                    selected = selected == period,
                    onClick = { onPick(period) },
                    label = { Text("P$period") },
                )
            }
            item(key = "clear") {
                androidx.compose.material3.AssistChip(
                    onClick = { onPick(null) },
                    label = { Text("Clear") },
                )
            }
        }
    }
}

/** Matches the eight default bells the view model and the web client both use. */
private const val PERIOD_COUNT = 8

@HiltViewModel
class ClassDetailViewModel @Inject constructor(
    private val academicRepository: AcademicRepository,
    private val peopleRepository: PeopleRepository,
    private val studentRepository: StudentRepository,
    private val accessStore: AccessStore,
    sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val classId: Long = checkNotNull(savedStateHandle[Routes.ARG_CLASS_ID])

    /**
     * Whether the caller may read this class's week at all. The endpoint behind the
     * Timetable tab is management-only server-side, so a teacher is not offered the
     * tab and the request is not made — otherwise a guaranteed 403 would arrive as
     * an empty grid reading "no timetable yet", which is a different claim.
     */
    private val isManagement = Role.from(sessionManager.currentUser?.role).isManagement

    private val _state = MutableStateFlow(
        ClassDetailUiState(
            isManagement = isManagement,
            // Read from AccessStore, not from the role: these grants are editable at
            // runtime on the Roles & Permissions screen, so a role list here would
            // offer controls the API refuses (or hide ones it would accept).
            canManageClasses = accessStore.can("CLASS_MANAGE"),
            canManageSection = accessStore.can("SECTION_MANAGE"),
            canManageSubjects = accessStore.can("SUBJECT_MANAGE"),
            canManageTimetable = accessStore.can("TIMETABLE_MANAGE"),
        ),
    )
    val state: StateFlow<ClassDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    /* ---- Writes -------------------------------------------------------------
     *
     * Each reloads afterwards rather than patching state in place: these actions
     * have server-side side effects a local edit cannot predict - assigning a class
     * teacher clears whoever held it, appointing a post ends the sitting tenure, and
     * the overview's setup warnings change with both.
     */

    private fun mutate(label: String, block: suspend () -> ApiResult<*>) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            when (val result = block()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isSaving = false, message = UiMessage.success(label)) }
                    load()
                }
                is ApiResult.Failure ->
                    // The server owns the real rules and its message says which one was
                    // broken - "Period 1 must be taught by the class teacher" is more
                    // use than anything guessable here.
                    _state.update { it.copy(isSaving = false, message = UiMessage.error(result.error)) }
            }
        }
    }

    fun assignClassTeacher(sectionId: Long, teacherId: Long) =
        mutate("Class teacher assigned.") { academicRepository.assignClassTeacher(sectionId, teacherId) }

    /**
     * The room and the capacity. Both optional: a class that has not been given a
     * room has none, which is different from being given an empty one, so a blank
     * field is sent as null rather than "".
     *
     * sectionName and classTeacherId are carried through from the record being
     * edited - the endpoint takes the whole section, so omitting them would clear
     * the name and unseat the class teacher.
     */
    fun updateSectionDetails(section: SectionDto, roomNumber: String, capacity: String) =
        mutate("Class details saved.") {
            academicRepository.updateSection(
                section.id,
                SectionRequestDto(
                    sectionName = section.sectionName,
                    roomNumber = roomNumber.trim().ifBlank { null },
                    capacity = capacity.trim().toIntOrNull(),
                    classTeacherId = section.classTeacherId,
                ),
            )
        }

    fun renameClass(className: String) =
        mutate("Class renamed.") {
            // The year is carried through for the same reason as the section name
            // above: the endpoint replaces the record rather than patching it.
            val yearId = _state.value.schoolClass?.academicYearId
            if (yearId == null) {
                ApiResult.Success(Unit)
            } else {
                academicRepository.updateClass(
                    classId,
                    SchoolClassRequestDto(className = className, academicYearId = yearId),
                )
            }
        }

    fun addSubject(name: String, code: String) =
        mutate("Subject added.") {
            academicRepository.createSubject(classId, SubjectRequestDto(subjectName = name, subjectCode = code))
        }

    fun deleteSubject(subjectId: Long) =
        mutate("Subject removed.") { academicRepository.deleteSubject(subjectId) }

    fun assignSubjectTeacher(sectionId: Long, subjectId: Long, teacherId: Long) =
        mutate("Teacher assigned.") {
            // uq_cst allows one teacher per subject per section, so an existing mapping
            // is removed first - otherwise the insert is rejected as a duplicate.
            _state.value.mappings
                .firstOrNull { it.subjectId == subjectId && it.sectionId == sectionId }
                ?.let { academicRepository.deleteTeacherMapping(it.id) }
            academicRepository.assignTeacherMapping(
                ClassSubjectTeacherRequestDto(
                    classId = classId,
                    sectionId = sectionId,
                    subjectId = subjectId,
                    teacherId = teacherId,
                ),
            )
        }

    fun removeSubjectTeacher(mappingId: Long) =
        mutate("Teacher unassigned.") { academicRepository.deleteTeacherMapping(mappingId) }

    fun appointOfficial(studentId: Long, role: String) =
        mutate("Appointed.") {
            academicRepository.appointClassOfficial(
                classId,
                ClassOfficialRequestDto(studentId = studentId, role = role),
            )
        }

    fun endOfficial(officialId: Long) =
        mutate("Post is now vacant.") { academicRepository.endClassOfficial(classId, officialId) }

    /**
     * Puts a subject at [periodNumber] on every working day at once — Monday to
     * Saturday, never Sunday.
     *
     * The shape almost every school timetable has, and it turns six identical picks
     * into one. The whole week is sent because the endpoint replaces it wholesale;
     * slots belonging to other subjects are carried through untouched, and only this
     * subject's rows are rebuilt.
     */
    fun applyPeriod(sectionId: Long, subjectId: Long, periodNumber: Int?) {
        val current = _state.value
        val teacherId = current.mappings
            .firstOrNull { it.subjectId == subjectId && it.sectionId == sectionId }
            ?.teacherId
        val others = current.timetable
            .filter { it.sectionId == sectionId && it.subjectId != subjectId }
            .map { it.toRequest() }
        val mine = if (periodNumber == null) {
            emptyList()
        } else {
            val bell = DEFAULT_PERIODS.getOrNull(periodNumber - 1) ?: DEFAULT_PERIODS.last()
            WORKING_DAYS.map { day ->
                TimetableSlotRequestDto(
                    dayOfWeek = day,
                    periodNumber = periodNumber,
                    startTime = bell.first,
                    endTime = bell.second,
                    subjectId = subjectId,
                    teacherId = teacherId,
                )
            }
        }
        mutate(if (periodNumber == null) "Periods cleared." else "Timetable saved.") {
            academicRepository.saveSectionTimetable(
                classId,
                sectionId,
                SaveTimetableRequestDto(slots = others + mine),
            )
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun TimetableSlotDto.toRequest() = TimetableSlotRequestDto(
        dayOfWeek = dayOfWeek,
        periodNumber = periodNumber,
        startTime = startTime ?: DEFAULT_PERIODS.first().first,
        endTime = endTime ?: DEFAULT_PERIODS.first().second,
        subjectId = subjectId,
        teacherId = teacherId,
        roomNumber = roomNumber,
        label = label,
    )

    private companion object {
        /** Sunday excluded: the school does not teach on it. Matches the web client. */
        val WORKING_DAYS = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")

        /**
         * Default bell times, start/end per period. The gaps are real - 11:00-11:20 is
         * the short break and 12:40-13:20 is lunch - so period 4 does not begin when
         * period 3 ends.
         */
        val DEFAULT_PERIODS = listOf(
            "09:00:00" to "09:40:00",
            "09:40:00" to "10:20:00",
            "10:20:00" to "11:00:00",
            "11:20:00" to "12:00:00",
            "12:00:00" to "12:40:00",
            "13:20:00" to "14:00:00",
            "14:00:00" to "14:40:00",
            "14:40:00" to "15:20:00",
        )
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            coroutineScope {
                // Four independent endpoints back the three tabs; fetching them together
                // means switching tabs is instant instead of triggering a new request.
                val classDeferred = async { academicRepository.getClass(classId) }
                val sections = async { academicRepository.getSections(classId).getOrNull().orEmpty() }
                val subjects = async { academicRepository.getSubjects(classId).getOrNull().orEmpty() }

                // The class-module tabs are fetched alongside the rest rather than on
                // first open, for the same reason: switching tabs stays instant. Each
                // falls back to empty on failure so one unavailable tab cannot take the
                // whole screen down - the class itself is the only required call.
                val overview = async { academicRepository.getClassOverview(classId).getOrNull() }
                val officials = async {
                    academicRepository.getClassOfficials(classId).getOrNull().orEmpty()
                }
                val officialHistory = async {
                    academicRepository.getClassOfficialHistory(classId).getOrNull().orEmpty()
                }
                val timetable = async {
                    if (isManagement) {
                        academicRepository.getClassTimetable(classId).getOrNull().orEmpty()
                    } else {
                        emptyList()
                    }
                }
                // Candidates for the class-teacher and subject-teacher pickers. Only
                // fetched for someone who can actually assign - a reader has no picker.
                val teachers = async {
                    if (accessStore.can("SECTION_MANAGE") || accessStore.can("SUBJECT_MANAGE")) {
                        peopleRepository.getTeachers(page = 0, size = 200).getOrNull()?.items.orEmpty()
                    } else {
                        emptyList()
                    }
                }
                // Candidates for a class post. Same reasoning: only someone who can
                // appoint has a picker to fill, and it is a page of its own to fetch.
                val students = async {
                    if (accessStore.can("CLASS_MANAGE")) {
                        studentRepository.getStudents(
                            classId = classId,
                            status = "ACTIVE",
                            size = 200,
                            sortBy = "rollNumber",
                        ).getOrNull()?.items.orEmpty()
                    } else {
                        emptyList()
                    }
                }

                when (val classResult = classDeferred.await()) {
                    is ApiResult.Success -> {
                        val loadedSections = sections.await()
                        // The mapping endpoint filters by section, so gather one call per
                        // section and flatten — there is no class-wide variant.
                        val mappings = loadedSections
                            .map { section ->
                                async {
                                    academicRepository.getTeacherMappings(sectionId = section.id)
                                        .getOrNull()
                                        .orEmpty()
                                }
                            }
                            .flatMap { it.await() }

                        // copy(), not a fresh state: the write gates are set in the
                        // initial value and every save calls load() again, so rebuilding
                        // the state here revoked them on the first reload - which emptied
                        // every picker the moment it was used.
                        _state.update {
                            it.copy(
                                schoolClass = classResult.data,
                                sections = loadedSections,
                                subjects = subjects.await(),
                                mappings = mappings,
                                overview = overview.await(),
                                officials = officials.await(),
                                officialHistory = officialHistory.await(),
                                timetable = timetable.await(),
                                teachers = teachers.await(),
                                students = students.await(),
                                isLoading = false,
                                error = null,
                            )
                        }
                    }

                    is ApiResult.Failure ->
                        _state.update { it.copy(isLoading = false, error = classResult.error) }
                }
            }
        }
    }
}

data class ClassDetailUiState(
    /** Drives whether the Timetable tab is offered — see the view model. */
    val isManagement: Boolean = false,
    /*
     * The write gates, each matching what the API enforces on the same action, so a
     * control is offered exactly when the request would be accepted:
     *
     *   CLASS_MANAGE     - the class itself, and appointing class posts
     *   SECTION_MANAGE   - room/capacity and the class-teacher assignment
     *   SUBJECT_MANAGE   - subjects, and the subject/teacher mapping
     *   TIMETABLE_MANAGE - the week. SUPER_ADMIN alone holds it by default.
     */
    val canManageClasses: Boolean = false,
    val canManageSection: Boolean = false,
    val canManageSubjects: Boolean = false,
    val canManageTimetable: Boolean = false,
    /** Candidates for the class-teacher and subject-teacher pickers. */
    val teachers: List<TeacherDto> = emptyList(),
    /** Candidates for a class post - this class's active students. */
    val students: List<StudentDto> = emptyList(),
    val isSaving: Boolean = false,
    val message: UiMessage? = null,
    val schoolClass: SchoolClassDto? = null,
    val sections: List<SectionDto> = emptyList(),
    val subjects: List<SubjectDto> = emptyList(),
    val mappings: List<ClassSubjectTeacherDto> = emptyList(),
    val overview: ClassOverviewDto? = null,
    val officials: List<ClassOfficialDto> = emptyList(),
    val officialHistory: List<ClassOfficialDto> = emptyList(),
    val timetable: List<TimetableSlotDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
)

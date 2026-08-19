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
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.data.remote.dto.ClassOfficialDto
import com.greenwood.school.data.remote.dto.ClassOverviewDto
import com.greenwood.school.data.remote.dto.ClassSubjectTeacherDto
import com.greenwood.school.data.remote.dto.SchoolClassDto
import com.greenwood.school.data.remote.dto.SectionDto
import com.greenwood.school.data.remote.dto.SubjectDto
import com.greenwood.school.data.remote.dto.TimetableSlotDto
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.navigation.Routes
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
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

    SimpleListScaffold(
        title = "Classes & Sections",
        subtitle = state.currentYearName,
        items = state.classes,
        isLoading = state.isLoading,
        error = state.error,
        onRefresh = viewModel::load,
        onBack = onBack,
        emptyTitle = "No classes yet",
        emptyMessage = "Classes appear here once the academic year is set up.",
        key = { it.id },
    ) { schoolClass ->
        EntityRowCard(
            title = schoolClass.className,
            subtitle = buildString {
                schoolClass.sectionCount?.let { append("$it section${if (it == 1) "" else "s"}") }
                schoolClass.studentCount?.let {
                    if (isNotEmpty()) append(" · ")
                    append("$it student${if (it == 1) "" else "s"}")
                }
            }.ifBlank { null },
            metadata = schoolClass.academicYearName,
            leadingInitials = schoolClass.className.filter { it.isDigit() }.ifBlank { "C" },
            showChevron = true,
            onClick = { onOpenClass(schoolClass.id) },
        )
    }
}

@HiltViewModel
class ClassListViewModel @Inject constructor(
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ClassListUiState())
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
                is ApiResult.Success -> _state.value = ClassListUiState(
                    classes = result.data,
                    currentYearName = year?.yearName,
                    isLoading = false,
                )

                is ApiResult.Failure -> _state.value = ClassListUiState(isLoading = false, error = result.error)
            }
        }
    }
}

data class ClassListUiState(
    val classes: List<SchoolClassDto> = emptyList(),
    val currentYearName: String? = null,
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
    val tabs = listOf("Overview", "Sections", "Subjects", "Teachers", "Posts", "Timetable")

    Scaffold(
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
                else -> when (tab) {
                    0 -> ClassOverviewTab(state.overview)
                    1 -> SectionsTab(state.sections)
                    2 -> SubjectsTab(state.subjects)
                    3 -> TeacherMappingTab(state.mappings)
                    4 -> ClassOfficialsTab(state.officials, state.officialHistory)
                    else -> ClassTimetableTab(state.timetable)
                }
            }
        }
    }
}

@Composable
private fun SectionsTab(sections: List<SectionDto>) {
    if (sections.isEmpty()) {
        EmptyView(title = "No sections", message = "This class has no sections yet.")
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sections.size, key = { sections[it].id }) { index ->
            val section = sections[index]
            EntityRowCard(
                title = "Section ${section.sectionName}",
                subtitle = section.classTeacherName?.let { "Class teacher: $it" }
                    ?: "No class teacher assigned",
                metadata = listOfNotNull(
                    section.roomNumber?.let { "Room $it" },
                    section.studentCount?.let { "$it students" },
                    section.capacity?.let { "Capacity $it" },
                ).joinToString(" · ").ifBlank { null },
                leadingInitials = section.sectionName.take(1),
            )
        }
    }
}

@Composable
private fun SubjectsTab(subjects: List<SubjectDto>) {
    if (subjects.isEmpty()) {
        EmptyView(title = "No subjects", message = "This class has no subjects yet.")
        return
    }
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
            )
        }
    }
}

@Composable
private fun TeacherMappingTab(mappings: List<ClassSubjectTeacherDto>) {
    if (mappings.isEmpty()) {
        EmptyView(
            title = "No subject teachers assigned",
            message = "Assign a teacher to each subject and section to see them here.",
        )
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(mappings.size, key = { mappings[it].id }) { index ->
            val mapping = mappings[index]
            EntityRowCard(
                title = mapping.teacherName ?: Formatters.PLACEHOLDER,
                subtitle = mapping.subjectName,
                metadata = mapping.sectionName?.let { "Section $it" },
                leadingInitials = Formatters.initials(mapping.teacherName.orEmpty()),
            )
        }
    }
}

@HiltViewModel
class ClassDetailViewModel @Inject constructor(
    private val academicRepository: AcademicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val classId: Long = checkNotNull(savedStateHandle[Routes.ARG_CLASS_ID])

    private val _state = MutableStateFlow(ClassDetailUiState())
    val state: StateFlow<ClassDetailUiState> = _state.asStateFlow()

    init {
        load()
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
                    academicRepository.getClassTimetable(classId).getOrNull().orEmpty()
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

                        _state.value = ClassDetailUiState(
                            schoolClass = classResult.data,
                            sections = loadedSections,
                            subjects = subjects.await(),
                            mappings = mappings,
                            overview = overview.await(),
                            officials = officials.await(),
                            officialHistory = officialHistory.await(),
                            timetable = timetable.await(),
                            isLoading = false,
                        )
                    }

                    is ApiResult.Failure ->
                        _state.value = ClassDetailUiState(isLoading = false, error = classResult.error)
                }
            }
        }
    }
}

data class ClassDetailUiState(
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

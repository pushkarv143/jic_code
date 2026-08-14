package com.greenwood.school.ui.feature.materials

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.data.remote.dto.StudyMaterialDto
import com.greenwood.school.domain.repository.ClassroomRepository
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.common.UiMessage
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.FilterChipRow
import com.greenwood.school.ui.components.PagedListScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// FilterChipRow renders its own "All" chip and represents it as a null selection,
// so this list holds only the real types.
private val TYPE_FILTERS = listOf(
    "Notes" to "NOTES",
    "Slides" to "PRESENTATION",
    "Worksheets" to "WORKSHEET",
    "Reference" to "REFERENCE",
    "Video" to "VIDEO",
)

/**
 * The study material library, read-only on mobile.
 *
 * Which materials appear is decided entirely by the backend — a teacher gets the
 * classes they teach including their own drafts, a student only published materials
 * for their own class and section — so this screen renders whatever it is given
 * without re-deriving any visibility rule.
 */
@Composable
fun StudyMaterialsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudyMaterialsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val list by viewModel.materials.collectAsStateWithLifecycle()
    val context = LocalContext.current

    PagedListScaffold(
        title = "Study Materials",
        subtitle = list.totalElements.takeIf { it > 0 }?.let { "${Formatters.number(it)} shared" },
        state = list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        searchQuery = state.search,
        onSearchChange = viewModel::onSearchChange,
        searchPlaceholder = "Search materials",
        hasActiveFilters = state.materialType != null,
        message = state.message,
        onMessageShown = viewModel::onMessageShown,
        emptyTitle = "No study materials yet",
        emptyMessage = "Nothing has been shared with your classes so far.",
        modifier = modifier,
        key = { it.id },
        header = {
            FilterChipRow(
                options = TYPE_FILTERS.map { it.first },
                selected = TYPE_FILTERS.firstOrNull { it.second == state.materialType }?.first,
                onSelected = { label ->
                    viewModel.onTypeSelected(TYPE_FILTERS.firstOrNull { it.first == label }?.second)
                },
            )
        },
    ) { material ->
        EntityRowCard(
            title = material.title,
            subtitle = listOfNotNull(
                material.subjectName,
                material.sectionName?.let { "${material.className.orEmpty()} $it".trim() }
                    ?: material.className?.let { "$it - all sections" },
            ).joinToString(" - ").ifBlank { null },
            metadata = buildString {
                append(material.materialType.lowercase().replaceFirstChar { it.uppercase() })
                material.teacherName?.let { append(" - shared by $it") }
                if (!material.published) append(" - draft")
            },
            showChevron = true,
            onClick = {
                // Goes back to the server rather than opening the URL already in
                // hand: the download endpoint re-runs the visibility check, so a
                // material revoked since the list loaded is refused rather than opened.
                viewModel.resolveAndOpen(material) { url ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }.onFailure { error ->
                        if (error is ActivityNotFoundException) {
                            viewModel.reportNoViewer()
                        } else {
                            throw error
                        }
                    }
                }
            },
            trailing = if (material.isExternalLink) {
                { Text("Link") }
            } else {
                null
            },
        )
    }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class StudyMaterialsViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyMaterialsUiState())
    val state: StateFlow<StudyMaterialsUiState> = _state.asStateFlow()

    private val loader = PagedLoader(viewModelScope) { page, size ->
        val current = _state.value
        classroomRepository.getStudyMaterials(
            materialType = current.materialType,
            search = current.search,
            page = page,
            size = size,
        )
    }
    val materials = loader.state

    init {
        loader.refresh()

        viewModelScope.launch {
            _state
                .map { it.search }
                .distinctUntilChanged()
                // Skip the initial empty value; refresh() above already covers it.
                .drop(1)
                .debounce(Constants.SEARCH_DEBOUNCE_MS)
                .collect { loader.refresh() }
        }
    }

    fun onSearchChange(value: String) = _state.update { it.copy(search = value) }

    fun onTypeSelected(type: String?) {
        _state.update { it.copy(materialType = type) }
        loader.refresh()
    }

    fun refresh() = loader.refresh()

    fun loadMore() = loader.loadMore()

    fun onMessageShown() = _state.update { it.copy(message = null) }

    fun reportNoViewer() = _state.update {
        it.copy(message = UiMessage("No app on this device can open that file.", isError = true))
    }

    fun resolveAndOpen(material: StudyMaterialDto, open: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = classroomRepository.resolveStudyMaterialDownload(material.id)) {
                is ApiResult.Success -> {
                    val url = result.data.resourceUrl
                    if (url.isNullOrBlank()) {
                        _state.update {
                            it.copy(message = UiMessage("This material has no file or link attached.", isError = true))
                        }
                    } else {
                        open(url)
                    }
                }
                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(message = UiMessage("You no longer have access to this material.", isError = true))
                    }
                    // The list is stale if access changed underneath us.
                    loader.refresh()
                }
            }
        }
    }
}

data class StudyMaterialsUiState(
    val search: String = "",
    val materialType: String? = null,
    val message: UiMessage? = null,
)

package com.greenwood.school.ui.feature.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.data.remote.dto.SchoolClassDto
import com.greenwood.school.data.remote.dto.SectionDto
import com.greenwood.school.data.remote.dto.StudentDto
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.domain.repository.StudentRepository
import com.greenwood.school.ui.common.PagedListState
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

/**
 * Paged, searchable, filterable student directory — the mobile equivalent of the
 * web `StudentListPage` DataGrid.
 *
 * Search is debounced so typing doesn't fire a request per keystroke; changing a
 * filter resets to page 0 and replaces the list rather than appending.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class StudentListViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StudentListUiState())
    val state: StateFlow<StudentListUiState> = _state.asStateFlow()

    init {
        loadFilters()
        refresh()

        viewModelScope.launch {
            _state
                .map { it.search }
                .distinctUntilChanged()
                // Skip the initial empty value; refresh() above already covers it.
                .drop(1)
                .debounce(Constants.SEARCH_DEBOUNCE_MS)
                .collect { refresh() }
        }
    }

    fun onSearchChange(value: String) = _state.update { it.copy(search = value) }

    fun onClassSelected(schoolClass: SchoolClassDto?) {
        _state.update { it.copy(selectedClass = schoolClass, selectedSection = null, sections = emptyList()) }
        schoolClass?.let { loadSections(it.id) }
        refresh()
    }

    fun onSectionSelected(section: SectionDto?) {
        _state.update { it.copy(selectedSection = section) }
        refresh()
    }

    fun onStatusSelected(status: String?) {
        _state.update { it.copy(status = status) }
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(list = it.list.startRefresh()) }
        loadPage(0)
    }

    fun loadMore() {
        val current = _state.value.list
        if (!current.canLoadMore) return
        _state.update { it.copy(list = it.list.startAppend()) }
        loadPage(current.page + 1)
    }

    private fun loadPage(page: Int) {
        val filters = _state.value
        viewModelScope.launch {
            val result = studentRepository.getStudents(
                search = filters.search,
                classId = filters.selectedClass?.id,
                sectionId = filters.selectedSection?.id,
                status = filters.status,
                page = page,
                size = Constants.DEFAULT_PAGE_SIZE,
            )
            _state.update { current ->
                current.copy(
                    list = when (result) {
                        is ApiResult.Success -> current.list.applyPage(result.data)
                        is ApiResult.Failure -> current.list.applyError(result.error)
                    },
                )
            }
        }
    }

    private fun loadFilters() {
        viewModelScope.launch {
            val classes = academicRepository.getClasses().getOrNull().orEmpty()
            _state.update { it.copy(classes = classes) }
        }
    }

    private fun loadSections(classId: Long) {
        viewModelScope.launch {
            val sections = academicRepository.getSections(classId).getOrNull().orEmpty()
            _state.update { it.copy(sections = sections) }
        }
    }
}

data class StudentListUiState(
    val list: PagedListState<StudentDto> = PagedListState(),
    val search: String = "",
    val classes: List<SchoolClassDto> = emptyList(),
    val sections: List<SectionDto> = emptyList(),
    val selectedClass: SchoolClassDto? = null,
    val selectedSection: SectionDto? = null,
    val status: String? = null,
) {
    val hasActiveFilters: Boolean
        get() = selectedClass != null || selectedSection != null || status != null
}

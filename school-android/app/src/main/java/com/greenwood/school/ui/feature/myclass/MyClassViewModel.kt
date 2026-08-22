package com.greenwood.school.ui.feature.myclass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.onFailure
import com.greenwood.school.core.network.onSuccess
import com.greenwood.school.core.session.AccessStore
import com.greenwood.school.data.remote.dto.ClassOfficialDto
import com.greenwood.school.data.remote.dto.HomeroomDto
import com.greenwood.school.data.remote.dto.MyClassOfficialRequestDto
import com.greenwood.school.data.remote.dto.MyClassStudentUpdateRequestDto
import com.greenwood.school.data.remote.dto.StudentDto
import com.greenwood.school.domain.repository.MyClassRepository
import com.greenwood.school.ui.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The class teacher's own section: roster, the records on it, and its class posts.
 *
 * <p>Capabilities come from [AccessStore], not from the role. Holding CLASS_TEACHER
 * is neither necessary nor sufficient — what matters is a row in
 * `sections.class_teacher_id`, which the server reports as
 * [AccessStore.isClassTeacherOfOwnSection].
 */
@HiltViewModel
class MyClassViewModel @Inject constructor(
    private val repository: MyClassRepository,
    private val accessStore: AccessStore,
) : ViewModel() {

    private val _state = MutableStateFlow(MyClassUiState())
    val state: StateFlow<MyClassUiState> = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                hasHomeroom = accessStore.isClassTeacherOfOwnSection,
                canEditStudents = accessStore.can("MY_CLASS_ROSTER_MANAGE"),
                canManageOfficials = accessStore.can("MY_CLASS_OFFICIALS_MANAGE"),
                homeroom = accessStore.homeroom,
            )
        }
        if (accessStore.isClassTeacherOfOwnSection) load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.myClass()
                .onSuccess { home -> _state.update { it.copy(homeroom = home) } }
                .onFailure { err -> _state.update { it.copy(error = err) } }

            repository.students(search = _state.value.search)
                .onSuccess { page -> _state.update { it.copy(students = page.items, total = page.totalElements) } }
                .onFailure { err -> _state.update { it.copy(error = err) } }

            // Non-fatal: the roster is the point of this screen, and the posts panel
            // renders its own empty state. A failure here must not blank the screen.
            repository.officials().onSuccess { list -> _state.update { it.copy(officials = list) } }

            _state.update { it.copy(isLoading = false) }
        }
    }

    fun search(term: String) {
        _state.update { it.copy(search = term) }
        load()
    }

    /**
     * Edits a student on the roster.
     *
     * <p>The request carries no class, section or roll number: the first two come
     * from the homeroom assignment server-side, and a roll number is assigned by the
     * server and unique per class. So this cannot move a pupil out of the class or
     * renumber them, only correct their details.
     */
    fun updateStudent(studentId: Long, request: MyClassStudentUpdateRequestDto) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            repository.updateStudent(studentId, request)
                .onSuccess {
                    _state.update { s -> s.copy(message = UiMessage.success("Student updated.")) }
                    load()
                }
                .onFailure { err -> _state.update { it.copy(error = err) } }
            _state.update { it.copy(isSaving = false) }
        }
    }

    fun appointOfficial(studentId: Long, role: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            repository.appointOfficial(MyClassOfficialRequestDto(studentId = studentId, role = role))
                .onSuccess {
                    _state.update { s -> s.copy(message = UiMessage.success("Appointed.")) }
                    load()
                }
                // The server owns the real rules — head boy must be male, a student
                // holds one post, the student must be in this class — so its message
                // is more useful than anything guessable here.
                .onFailure { err -> _state.update { it.copy(error = err) } }
            _state.update { it.copy(isSaving = false) }
        }
    }

    fun endOfficial(officialId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            repository.endOfficial(officialId)
                .onSuccess {
                    _state.update { s -> s.copy(message = UiMessage.success("Post is now vacant.")) }
                    load()
                }
                .onFailure { err -> _state.update { it.copy(error = err) } }
            _state.update { it.copy(isSaving = false) }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}

data class MyClassUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    /**
     * False when the signed-in user is class teacher of no section — true for 27 of
     * the 44 CLASS_TEACHER role holders on the seeded database. The menu entry is
     * hidden for them; this is what the screen itself checks if reached directly.
     */
    val hasHomeroom: Boolean = false,
    val canEditStudents: Boolean = false,
    val canManageOfficials: Boolean = false,
    val homeroom: HomeroomDto? = null,
    val students: List<StudentDto> = emptyList(),
    val total: Long = 0,
    val officials: List<ClassOfficialDto> = emptyList(),
    val search: String = "",
    val error: AppError? = null,
    val message: UiMessage? = null,
)

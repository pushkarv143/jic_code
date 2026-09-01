package com.greenwood.school.ui.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.data.remote.dto.GlobalSearchResponseDto
import com.greenwood.school.data.remote.dto.GlobalSearchResultItemDto
import com.greenwood.school.domain.repository.SettingsRepository
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.SearchField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Global search across students, teachers and books — the mobile form of the web
 * top-bar search dropdown, promoted to a full screen because a dropdown over a
 * phone keyboard has nowhere to render.
 *
 * The backend caps results at five per category, so the whole response fits in
 * one screen and there is nothing to page.
 */
@Composable
fun SearchScreen(
    onOpenStudent: (Long) -> Unit,
    onOpenTeacher: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { AppTopBar(title = "Search", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Students, teachers, books…",
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            when {
                state.query.length < MIN_QUERY_LENGTH -> EmptyView(
                    title = "Search anything",
                    message = "Find students, teachers or books — enter at least $MIN_QUERY_LENGTH characters.",
                    icon = Icons.Outlined.Search,
                )

                state.isLoading -> FullScreenLoader()

                state.error != null -> ErrorView(error = state.error!!, onRetry = viewModel::search)

                state.results == null || state.results!!.isEmpty -> EmptyView(
                    title = "No matches",
                    message = "Nothing found for \"${state.query}\".",
                )

                else -> ResultList(
                    results = state.results!!,
                    onOpenStudent = onOpenStudent,
                    onOpenTeacher = onOpenTeacher,
                )
            }
        }
    }
}

@Composable
private fun ResultList(
    results: GlobalSearchResponseDto,
    onOpenStudent: (Long) -> Unit,
    onOpenTeacher: (Long) -> Unit,
) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        section("Students", results.students) { item ->
            ResultRow(item) { onOpenStudent(item.id) }
        }
        section("Teachers", results.teachers) { item ->
            ResultRow(item) { onOpenTeacher(item.id) }
        }
        // Books have no detail screen of their own; they resolve in the Library tab.
        section("Books", results.books) { item -> ResultRow(item, onClick = null) }
    }
}

/**
 * The parameter is named `rows`, not `items`, so it cannot shadow
 * `LazyListScope.items` — with both in scope the call below is ambiguous.
 */
private fun LazyListScope.section(
    title: String,
    rows: List<GlobalSearchResultItemDto>,
    row: @Composable (GlobalSearchResultItemDto) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "header-$title") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    items(rows.size, key = { "$title-${rows[it].id}" }) { index -> row(rows[index]) }
}

@Composable
private fun ResultRow(item: GlobalSearchResultItemDto, onClick: (() -> Unit)?) {
    EntityRowCard(
        title = item.label,
        subtitle = item.secondary,
        leadingInitials = Formatters.initials(item.label),
        showChevron = onClick != null,
        onClick = onClick,
    )
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state
                .map { it.query }
                .distinctUntilChanged()
                .debounce(Constants.SEARCH_DEBOUNCE_MS)
                .collect { query -> if (query.length >= MIN_QUERY_LENGTH) search() }
        }
    }

    fun onQueryChange(value: String) = _state.update {
        it.copy(
            query = value,
            // Clear stale results the moment the query drops below the threshold, so
            // the screen never shows matches for something the user has deleted.
            results = if (value.length < MIN_QUERY_LENGTH) null else it.results,
        )
    }

    fun search() {
        val query = _state.value.query
        if (query.length < MIN_QUERY_LENGTH) return

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = settingsRepository.globalSearch(query)) {
                is ApiResult.Success -> _state.update {
                    // Guard against an out-of-order response for an older query.
                    if (it.query == query) it.copy(isLoading = false, results = result.data) else it
                }

                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}

data class SearchUiState(
    val query: String = "",
    val results: GlobalSearchResponseDto? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
)

private const val MIN_QUERY_LENGTH = 2

package com.greenwood.school.ui.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.data.remote.dto.BookDto
import com.greenwood.school.data.remote.dto.BookIssueDto
import com.greenwood.school.data.remote.dto.LibraryDashboardDto
import com.greenwood.school.domain.repository.LibraryRepository
import com.greenwood.school.ui.common.PagedListState
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.common.UiMessage
import com.greenwood.school.ui.components.AppCard
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.LoadMoreFooter
import com.greenwood.school.ui.components.SearchField
import com.greenwood.school.ui.components.StatusChip
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
import java.time.LocalDate
import javax.inject.Inject

/**
 * Library, as three tabs: catalogue, issued books, overdue. The librarian
 * dashboard counters sit above the tabs so the numbers that matter are visible
 * regardless of which tab is open — the web app puts them on a separate page.
 */
@Composable
fun LibraryScreen(
    onBack: (() -> Unit)? = null,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val issues by viewModel.issues.collectAsStateWithLifecycle()
    val overdue by viewModel.overdue.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message?.id) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Library", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.dashboard?.let { DashboardStrip(it) }

            TabRow(selectedTabIndex = tab) {
                listOf("Catalogue", "Issued", "Overdue").forEachIndexed { index, label ->
                    Tab(
                        selected = tab == index,
                        onClick = {
                            tab = index
                            when (index) {
                                1 -> viewModel.refreshIssues()
                                2 -> viewModel.refreshOverdue()
                            }
                        },
                        text = { Text(label) },
                    )
                }
            }

            when (tab) {
                0 -> CatalogueTab(state.search, books, viewModel)
                1 -> IssueTab(issues, viewModel, showReturn = true)
                else -> IssueTab(overdue, viewModel, showReturn = true, isOverdue = true)
            }
        }
    }
}

@Composable
private fun DashboardStrip(dashboard: LibraryDashboardDto) {
    AppCard(Modifier.fillMaxWidth().padding(12.dp)) {
        Row(Modifier.padding(14.dp)) {
            Figure("Titles", Formatters.number(dashboard.totalBooks), Modifier.weight(1f))
            Figure("Available", Formatters.number(dashboard.availableCopies), Modifier.weight(1f))
            Figure("Issued", Formatters.number(dashboard.issuedCount), Modifier.weight(1f))
            Figure("Overdue", Formatters.number(dashboard.overdueCount), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CatalogueTab(
    search: String,
    books: PagedListState<BookDto>,
    viewModel: LibraryViewModel,
) {
    Column(Modifier.fillMaxSize()) {
        SearchField(
            value = search,
            onValueChange = viewModel::onSearchChange,
            placeholder = "Search by title, author or ISBN",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        when {
            books.isInitialLoad -> FullScreenLoader()
            books.error != null -> ErrorView(error = books.error, onRetry = viewModel::refreshBooks)
            books.isEmpty -> EmptyView(title = "No books found")
            else -> LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(books.items.size, key = { books.items[it].id }) { index ->
                    val book = books.items[index]
                    EntityRowCard(
                        title = book.title,
                        subtitle = book.author,
                        metadata = listOfNotNull(book.categoryName, book.isbn, book.rackNumber?.let { "Rack $it" })
                            .joinToString(" · ")
                            .ifBlank { null },
                        leadingInitials = Formatters.initials(book.title),
                        trailing = {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                Text(
                                    "${book.availableCopies}/${book.totalCopies}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    if (book.isAvailable) "available" else "all out",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
                item { LoadMoreFooter(books.isAppending, books.appendError, viewModel::loadMoreBooks) }
            }
        }
    }
}

@Composable
private fun IssueTab(
    issues: PagedListState<BookIssueDto>,
    viewModel: LibraryViewModel,
    showReturn: Boolean,
    isOverdue: Boolean = false,
) {
    when {
        issues.isInitialLoad -> FullScreenLoader()
        issues.error != null -> ErrorView(
            error = issues.error,
            onRetry = if (isOverdue) viewModel::refreshOverdue else viewModel::refreshIssues,
        )

        issues.isEmpty -> EmptyView(
            title = if (isOverdue) "Nothing overdue" else "No books issued",
            message = if (isOverdue) "Every issued book is within its due date." else null,
        )

        else -> LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(issues.items.size, key = { issues.items[it].id }) { index ->
                val issue = issues.items[index]
                EntityRowCard(
                    title = issue.bookTitle ?: "Book #${issue.bookId}",
                    subtitle = issue.borrowerName,
                    metadata = buildString {
                        append("Issued ${Formatters.date(issue.issueDate)}")
                        append(" · due ${Formatters.date(issue.dueDate)}")
                        if (issue.fineAmount > 0) append(" · fine ${Formatters.currency(issue.fineAmount)}")
                    },
                    leadingInitials = Formatters.initials(issue.bookTitle.orEmpty()),
                    trailing = {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            StatusChip(issue.status)
                            if (showReturn && issue.status != "RETURNED") {
                                TextButton(onClick = { viewModel.returnBook(issue) }) { Text("Return") }
                            }
                        }
                    },
                )
            }
            item {
                LoadMoreFooter(
                    issues.isAppending,
                    issues.appendError,
                    if (isOverdue) viewModel::loadMoreOverdue else viewModel::loadMoreIssues,
                )
            }
        }
    }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    private val bookLoader = PagedLoader(viewModelScope) { page, size ->
        libraryRepository.getBooks(search = _state.value.search, page = page, size = size)
    }
    val books = bookLoader.state

    private val issueLoader = PagedLoader(viewModelScope) { page, size ->
        libraryRepository.getIssues(status = "ISSUED", page = page, size = size)
    }
    val issues = issueLoader.state

    private val overdueLoader = PagedLoader(viewModelScope) { page, size ->
        libraryRepository.getOverdue(page = page, size = size)
    }
    val overdue = overdueLoader.state

    init {
        bookLoader.refresh()
        loadDashboard()
        viewModelScope.launch {
            _state.map { it.search }.distinctUntilChanged().drop(1)
                .debounce(Constants.SEARCH_DEBOUNCE_MS)
                .collect { bookLoader.refresh() }
        }
    }

    fun onSearchChange(value: String) = _state.update { it.copy(search = value) }

    fun returnBook(issue: BookIssueDto) {
        viewModelScope.launch {
            val result = libraryRepository.returnBook(issue.id, Formatters.apiDate(LocalDate.now()))
            when (result) {
                is ApiResult.Success -> {
                    // A returned book leaves both the issued and overdue lists.
                    issueLoader.removeWhere { it.id == issue.id }
                    overdueLoader.removeWhere { it.id == issue.id }
                    _state.update {
                        it.copy(
                            message = UiMessage.success(
                                buildString {
                                    append("Returned.")
                                    if (result.data.fineAmount > 0) {
                                        append(" Fine due: ${Formatters.currency(result.data.fineAmount)}")
                                    }
                                },
                            ),
                        )
                    }
                    loadDashboard()
                    bookLoader.refresh()
                }

                is ApiResult.Failure -> _state.update { it.copy(message = UiMessage.error(result.error)) }
            }
        }
    }

    private fun loadDashboard() = viewModelScope.launch {
        _state.update { it.copy(dashboard = libraryRepository.getDashboard().getOrNull()) }
    }

    fun refreshBooks() = bookLoader.refresh()
    fun loadMoreBooks() = bookLoader.loadMore()
    fun refreshIssues() = issueLoader.refresh()
    fun loadMoreIssues() = issueLoader.loadMore()
    fun refreshOverdue() = overdueLoader.refresh()
    fun loadMoreOverdue() = overdueLoader.loadMore()
    fun consumeMessage() = _state.update { it.copy(message = null) }
}

data class LibraryUiState(
    val search: String = "",
    val dashboard: LibraryDashboardDto? = null,
    val message: UiMessage? = null,
)

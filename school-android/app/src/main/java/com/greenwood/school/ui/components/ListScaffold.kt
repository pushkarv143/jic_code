package com.greenwood.school.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.ui.common.PagedListState
import com.greenwood.school.ui.common.UiMessage

/**
 * The frame every paged list screen shares: title bar, optional search box,
 * optional filter sheet, the four list states, and a snackbar.
 *
 * Screens supply only the row composable and whatever goes in the filter sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PagedListScaffold(
    title: String,
    state: PagedListState<T>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    searchQuery: String? = null,
    onSearchChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = "Search",
    hasActiveFilters: Boolean = false,
    message: UiMessage? = null,
    onMessageShown: () -> Unit = {},
    emptyTitle: String = "Nothing here yet",
    emptyMessage: String? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    onAdd: (() -> Unit)? = null,
    addContentDescription: String = "Add",
    filterSheet: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
    key: ((T) -> Any)? = null,
    header: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message?.id) {
        message?.let {
            snackbarHostState.showSnackbar(it.text)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = title,
                subtitle = subtitle
                    ?: state.totalElements.takeIf { it > 0 }?.let { "${Formatters.number(it)} records" },
                onBack = onBack,
                actions = {
                    actions()
                    if (filterSheet != null) {
                        IconButton(onClick = { showFilters = true }) {
                            Icon(
                                Icons.Outlined.FilterList,
                                contentDescription = "Filters",
                                tint = if (hasActiveFilters) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (onAdd != null) {
                FloatingActionButton(onClick = onAdd) {
                    Icon(Icons.Outlined.Add, addContentDescription)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (searchQuery != null && onSearchChange != null) {
                SearchField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = searchPlaceholder,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            when {
                state.isInitialLoad -> FullScreenLoader()

                state.error != null -> ErrorView(error = state.error, onRetry = onRefresh)

                state.isEmpty -> EmptyView(title = emptyTitle, message = emptyMessage)

                else -> PagedLazyColumn(
                    state = state,
                    onLoadMore = onLoadMore,
                    key = key,
                    header = header,
                    itemContent = itemContent,
                )
            }
        }
    }

    if (showFilters && filterSheet != null) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }, sheetState = sheetState) {
            filterSheet { showFilters = false }
        }
    }
}

/**
 * Same frame for screens whose data is a plain (unpaged) list — the many endpoints
 * that return `ApiResponse<List<T>>` rather than a page: departments, fee
 * categories, drivers, buses, hostels, exam types and so on.
 */
@Composable
fun <T> SimpleListScaffold(
    title: String,
    items: List<T>,
    isLoading: Boolean,
    error: com.greenwood.school.core.network.AppError?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    message: UiMessage? = null,
    onMessageShown: () -> Unit = {},
    emptyTitle: String = "Nothing here yet",
    emptyMessage: String? = null,
    onAdd: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(12.dp),
    key: ((T) -> Any)? = null,
    header: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message?.id) {
        message?.let {
            snackbarHostState.showSnackbar(it.text)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = title, subtitle = subtitle, onBack = onBack, actions = actions) },
        floatingActionButton = {
            if (onAdd != null) {
                FloatingActionButton(onClick = onAdd) {
                    Icon(Icons.Outlined.Add, "Add")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading && items.isEmpty() -> FullScreenLoader()
                error != null && items.isEmpty() -> ErrorView(error = error, onRetry = onRefresh)
                items.isEmpty() -> EmptyView(title = emptyTitle, message = emptyMessage)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (header != null) item(key = "header") { header() }
                    items(
                        count = items.size,
                        key = if (key != null) { index -> key(items[index]) } else null,
                    ) { index -> itemContent(items[index]) }
                }
            }
        }
    }
}

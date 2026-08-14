package com.greenwood.school.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.network.AppError
import com.greenwood.school.ui.common.PagedListState
import com.greenwood.school.ui.theme.StatusPalette
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column2(title = title, subtitle = subtitle)
        },
        navigationIcon = {
            when {
                navigationIcon != null -> navigationIcon()
                onBack != null -> IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun Column2(title: String, subtitle: String?) {
    androidx.compose.foundation.layout.Column {
        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Colour-coded status pill. Every enum in the system — attendance, fee, leave,
 * payroll, book issue — funnels through [StatusPalette] so the same word always
 * gets the same colour, exactly as the web app's chip styling does.
 */
@Composable
fun StatusChip(status: String?, modifier: Modifier = Modifier) {
    if (status.isNullOrBlank()) return
    val color = StatusPalette.forStatus(status)
    AssistChip(
        onClick = {},
        enabled = false,
        modifier = modifier,
        label = { Text(Formatters.humanizeEnum(status), style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color.copy(alpha = 0.12f),
            disabledLabelColor = color,
        ),
        border = null,
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Infinite-scrolling list wired to [PagedListState].
 *
 * Pages are requested when the user is within [prefetchDistance] rows of the end,
 * so the next page is usually already there by the time they reach it. This is the
 * mobile answer to the web app's numbered DataGrid pager.
 */
@Composable
fun <T> PagedLazyColumn(
    state: PagedListState<T>,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    prefetchDistance: Int = 4,
    key: ((T) -> Any)? = null,
    header: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    val shouldLoadMore by remember(state) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            state.canLoadMore && lastVisible >= state.items.lastIndex - prefetchDistance
        }
    }

    LaunchedEffect(listState, state.page, state.canLoadMore) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        if (header != null) {
            item(key = "header") { header() }
        }
        items(
            count = state.items.size,
            key = if (key != null) { index -> key(state.items[index]) } else null,
        ) { index ->
            itemContent(state.items[index])
        }
        if (state.isAppending || state.appendError != null) {
            item(key = "footer") {
                LoadMoreFooter(
                    isLoading = state.isAppending,
                    error = state.appendError,
                    onRetry = onLoadMore,
                )
            }
        }
    }
}

/** Small right-aligned "12 of 340" caption above a paged list. */
@Composable
fun ResultCount(shown: Int, total: Long, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
        Text(
            text = "Showing ${Formatters.number(shown)} of ${Formatters.number(total)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Convenience for turning an [AppError] into a snackbar-friendly string. */
fun AppError.snackbarText(): String = userMessage

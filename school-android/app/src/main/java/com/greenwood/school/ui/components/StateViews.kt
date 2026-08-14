package com.greenwood.school.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.isRetryable
import com.greenwood.school.ui.common.UiState

/**
 * Renders the loading / empty / error branches so each screen only has to describe
 * its *content*. This is the single place the app decides what "no internet" or
 * "permission denied" looks like.
 */
@Composable
fun <T> StateHost(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    emptyTitle: String = "Nothing here yet",
    emptyMessage: String? = null,
    emptyAction: (@Composable () -> Unit)? = null,
    loading: @Composable () -> Unit = { FullScreenLoader() },
    content: @Composable (T) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            is UiState.Loading -> loading()
            is UiState.Empty -> EmptyView(title = emptyTitle, message = emptyMessage, action = emptyAction)
            is UiState.Error -> ErrorView(error = state.error, onRetry = onRetry)
            is UiState.Success -> content(state.data)
        }
    }
}

@Composable
fun FullScreenLoader(label: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        if (label != null) {
            Spacer(Modifier.height(16.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyView(
    title: String,
    message: String? = null,
    icon: ImageVector = Icons.Outlined.Inbox,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    IllustratedMessage(
        icon = icon,
        title = title,
        message = message,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
        action = action,
    )
}

/**
 * Error presentation is driven entirely by the [AppError] case: a network problem
 * gets a different icon, wording and affordance than a 403, and only genuinely
 * retryable failures offer a Retry button.
 */
@Composable
fun ErrorView(error: AppError, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    val icon = when (error) {
        is AppError.Network -> Icons.Outlined.CloudOff
        is AppError.Forbidden, is AppError.Unauthorized -> Icons.Outlined.Lock
        is AppError.NotFound -> Icons.Outlined.SearchOff
        else -> Icons.Outlined.WarningAmber
    }

    val title = when (error) {
        is AppError.Network -> "You're offline"
        is AppError.Timeout -> "That took too long"
        is AppError.Forbidden -> "No access"
        is AppError.Unauthorized -> "Session expired"
        is AppError.NotFound -> "Not found"
        else -> "Something went wrong"
    }

    IllustratedMessage(
        icon = icon,
        title = title,
        message = error.userMessage,
        tint = if (error is AppError.Forbidden) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        },
        modifier = modifier,
        action = {
            // A 403 will keep being a 403 — offering Retry there is a dead end.
            if (onRetry != null && error.isRetryable) {
                Button(onClick = onRetry) { Text("Retry") }
            } else if (onRetry != null && error !is AppError.Forbidden) {
                OutlinedButton(onClick = onRetry) { Text("Try again") }
            }
        },
    )
}

@Composable
private fun IllustratedMessage(
    icon: ImageVector,
    title: String,
    message: String?,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/**
 * Persistent strip shown above content while the device has no connectivity.
 * Unlike [ErrorView] this never replaces content — stale data beats a blank screen.
 */
@Composable
fun OfflineBanner(visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "You're offline — showing the last loaded data.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Footer for an infinite list: spinner, or a retry row when the append failed. */
@Composable
fun LoadMoreFooter(
    isLoading: Boolean,
    error: AppError?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> Box(
            modifier = modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }

        error != null -> Column(
            modifier = modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                error.userMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

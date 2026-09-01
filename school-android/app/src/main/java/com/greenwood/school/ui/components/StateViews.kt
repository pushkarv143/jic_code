package com.greenwood.school.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.isRetryable
import com.greenwood.school.ui.common.UiState
import com.greenwood.school.ui.theme.BrandAmber
import com.greenwood.school.ui.theme.BrandIndigo

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

/**
 * Branded full-screen loader: school icon badge + amber LinearProgress.
 */
@Composable
fun FullScreenLoader(label: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Branded icon badge
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(BrandIndigo, BrandIndigo.copy(alpha = 0.7f))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .width(140.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(100.dp)),
            color = BrandAmber,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
        if (label != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
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
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier,
        action = action,
    )
}

/**
 * Error presentation driven entirely by the [AppError] case.
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
    val tint = if (error is AppError.Forbidden)
        MaterialTheme.colorScheme.onSurfaceVariant
    else
        MaterialTheme.colorScheme.error

    IllustratedMessage(
        icon = icon,
        title = title,
        message = error.userMessage,
        tint = tint,
        modifier = modifier,
        action = {
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
    tint: Color,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Gradient icon container
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(tint.copy(alpha = 0.15f), tint.copy(alpha = 0.05f))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

/** Footer for an infinite list: slim progress bar, or a retry row when the append failed. */
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
            LinearProgressIndicator(
                modifier = Modifier
                    .width(80.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(100.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )
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

package com.greenwood.school.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.ui.theme.Elevation
import com.greenwood.school.ui.theme.NumericType
import com.greenwood.school.ui.theme.Sizing
import com.greenwood.school.ui.theme.spacing

/**
 * Headline metric tile — the mobile equivalent of the web app's `StatCard`.
 * Dashboards lay these out two-per-row rather than the web's six-across grid.
 *
 * The figure uses [NumericType.statValue] rather than a headline style so the
 * number stays fixed-width as it updates and the label beneath it doesn't shuffle.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    caption: String? = null,
    onClick: (() -> Unit)? = null,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Box {
            // Top accent stripe — 3dp gradient bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accent, accent.copy(alpha = 0.3f)),
                        )
                    )
            )
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 14.dp)) {
                // Gradient icon tile
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.18f),
                                    accent.copy(alpha = 0.06f),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = value,
                    style = NumericType.statValue,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (caption != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = caption,
                        style = NumericType.statCaption,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * The workhorse list row: avatar/initials, a title, up to two metadata lines and
 * an optional trailing slot. Web data grids become stacks of these on mobile —
 * a table with eight columns is unusable on a phone, so the two or three columns
 * that actually identify a record get promoted and the rest move to the detail screen.
 */
@Composable
fun EntityRowCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    metadata: String? = null,
    imageUrl: String? = null,
    leadingInitials: String? = null,
    leadingAccent: Color = MaterialTheme.colorScheme.primary,
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .sizeIn(minHeight = Sizing.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (imageUrl != null || leadingInitials != null) {
                Avatar(
                    imageUrl = imageUrl,
                    initials = leadingInitials ?: Formatters.initials(title),
                    accent = leadingAccent,
                    size = 44.dp,
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!metadata.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
            if (showChevron) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun Avatar(
    initials: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    size: Dp = Sizing.avatar,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (!imageUrl.isNullOrBlank())
                    Modifier.background(accent.copy(alpha = 0.12f))
                else
                    Modifier.background(
                        Brush.linearGradient(colors = listOf(accent, accent.copy(alpha = 0.65f)))
                    )
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape),
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

/** Titled container with gradient header divider. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                action?.invoke()
            }
            // Gradient divider under header
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            )
                        )
                    )
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Label-Value detail row. */
@Composable
fun DetailRow(label: String, value: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: Formatters.PLACEHOLDER,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.4f),
        )
    }
}

/**
 * Enhanced card chrome: 16dp radius, hairline outline, spring press animation.
 * Pressed state lifts elevation AND applies subtle scale for tactile feedback.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val shape = MaterialTheme.shapes.medium
    val border = BorderStroke(Sizing.hairline, MaterialTheme.colorScheme.outlineVariant)

    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1),
            border = border,
        ) { content() }
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 1000f),
        label = "card-press",
    )

    Card(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = shape,
        colors = colors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = Elevation.level1,
            pressedElevation = Elevation.level3,
        ),
        border = border,
        interactionSource = interactionSource,
    ) { content() }
}

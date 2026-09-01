package com.greenwood.school.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.greenwood.school.ui.theme.Sizing
import com.greenwood.school.ui.theme.extraColors
import com.greenwood.school.ui.theme.spacing

/**
 * Skeleton placeholders for first load.
 *
 * A centred spinner tells the user "something is happening"; a skeleton tells them
 * *what* is about to appear and roughly how much of it, so the screen doesn't jump
 * when content lands. Use these wherever the shape of the result is known in
 * advance — which on a dashboard it always is.
 *
 * Reach for [FullScreenLoader] instead only when the layout genuinely can't be
 * predicted (a role-dependent screen before the role is known, say).
 */

/**
 * A sweeping highlight, drawn behind the placeholder.
 *
 * The gradient is translated from off-screen left to off-screen right rather than
 * animating its colours, which keeps the whole effect to one animated float no
 * matter how many placeholders are on screen — all of them share this transition.
 */
@Composable
private fun Modifier.shimmer(): Modifier {
    val base = MaterialTheme.extraColors.shimmerBase
    val highlight = MaterialTheme.extraColors.shimmerHighlight

    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
        ),
        label = "shimmer-progress",
    )

    return drawWithCache {
        val width = size.width
        // Travels from one full width left of the shape to one full width right,
        // so the highlight enters and exits cleanly instead of popping.
        val start = (progress * 2f - 1f) * width
        val brush = Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(start, 0f),
            end = Offset(start + width, 0f),
        )
        onDrawBehind { drawRect(brush) }
    }
}

/** A single shimmering block. The building block for every skeleton below. */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Box(modifier.clip(shape).shimmer())
}

/** A shimmering text line. [widthFraction] fakes the ragged edge of real prose. */
@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    widthFraction: Float = 1f,
) {
    SkeletonBox(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height),
        shape = RoundedCornerShape(6.dp),
    )
}

/** Placeholder matching [com.greenwood.school.ui.components.StatCard]'s footprint. */
@Composable
fun SkeletonStatCard(modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(Modifier.padding(MaterialTheme.spacing.md)) {
            SkeletonBox(
                modifier = Modifier.size(Sizing.iconTile),
                shape = MaterialTheme.shapes.small,
            )
            Spacer(Modifier.height(MaterialTheme.spacing.md))
            SkeletonLine(height = 24.dp, widthFraction = 0.7f)
            Spacer(Modifier.height(MaterialTheme.spacing.sm))
            SkeletonLine(height = 12.dp, widthFraction = 0.9f)
        }
    }
}

/** Placeholder for a titled section with [lines] rows of content. */
@Composable
fun SkeletonSectionCard(
    modifier: Modifier = Modifier,
    lines: Int = 3,
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(MaterialTheme.spacing.md)) {
            SkeletonLine(height = 18.dp, widthFraction = 0.45f)
            Spacer(Modifier.height(MaterialTheme.spacing.md))
            repeat(lines) { index ->
                SkeletonLine(height = 14.dp, widthFraction = 0.95f)
                Spacer(Modifier.height(MaterialTheme.spacing.xs))
                SkeletonLine(height = 12.dp, widthFraction = 0.6f)
                if (index != lines - 1) Spacer(Modifier.height(MaterialTheme.spacing.md))
            }
        }
    }
}

/** Placeholder for an avatar + two-line list row. */
@Composable
fun SkeletonRow(modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            SkeletonBox(Modifier.size(Sizing.avatar), CircleShape)
            Spacer(Modifier.size(MaterialTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                SkeletonLine(height = 14.dp, widthFraction = 0.55f)
                Spacer(Modifier.height(MaterialTheme.spacing.sm))
                SkeletonLine(height = 12.dp, widthFraction = 0.35f)
            }
        }
    }
}

/**
 * The dashboard's own first-load state: a stat grid over two section cards,
 * mirroring what actually arrives.
 *
 * Marked as one semantics node so TalkBack announces "Loading dashboard" once
 * rather than reading out a dozen meaningless decorative boxes.
 */
@Composable
fun DashboardSkeleton(
    modifier: Modifier = Modifier,
    statTiles: Int = 4,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.md)
            .semantics { contentDescription = "Loading dashboard" },
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        repeat(statTiles / 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                SkeletonStatCard(Modifier.weight(1f))
                SkeletonStatCard(Modifier.weight(1f))
            }
        }
        SkeletonSectionCard(lines = 3)
        SkeletonSectionCard(lines = 2)
    }
}

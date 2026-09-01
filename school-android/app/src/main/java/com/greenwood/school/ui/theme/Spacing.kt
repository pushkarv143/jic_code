package com.greenwood.school.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The 8dp grid, as tokens.
 *
 * Before this existed the app padded things with 5, 6, 10, 12 and 14dp depending on
 * who wrote the screen, which is why nothing quite lined up between cards. Every
 * value below is a multiple of 8, except [xs] which is the half-step the grid allows
 * for tight optical corrections (icon-to-label, chip innards).
 *
 * Read them through [MaterialTheme.spacing] rather than importing the object, so a
 * future compact/expanded window class can swap the whole scale in one place.
 */
@androidx.compose.runtime.Immutable
data class Spacing(
    /** 4dp — the only sub-grid value. Icon-to-label, chip padding. */
    val xs: Dp = 4.dp,
    /** 8dp — gap between tightly related items. */
    val sm: Dp = 8.dp,
    /** 16dp — the default. Card interiors, screen gutters, list gaps. */
    val md: Dp = 16.dp,
    /** 24dp — separates sections within a screen. */
    val lg: Dp = 24.dp,
    /** 32dp — major breaks, empty-state insets. */
    val xl: Dp = 32.dp,
    /** 48dp — hero spacing, bottom scroll runway. */
    val xxl: Dp = 48.dp,
) {
    /** Horizontal inset from the screen edge. Kept as its own name because it is a
     *  layout decision, not a spacing step — changing it should not move card interiors. */
    val gutter: Dp get() = md

    companion object {
        /** Phones. */
        val Compact = Spacing()

        /** Tablets and unfolded foldables — the same rhythm, one step roomier. */
        val Expanded = Spacing(
            xs = 4.dp, sm = 12.dp, md = 24.dp, lg = 32.dp, xl = 48.dp, xxl = 64.dp,
        )
    }
}

/**
 * Elevation ladder. Material 3 conveys depth mostly through tonal surface colour,
 * so these stay deliberately low — a 1dp card with a tonal fill reads as "lifted"
 * without the muddy drop shadow that made the old cards look like 2014 Material.
 */
object Elevation {
    /** Flush with the background — dividers do the work instead. */
    val level0: Dp = 0.dp
    /** Resting cards and list rows. */
    val level1: Dp = 1.dp
    /** Cards that respond to touch, raised chips. */
    val level2: Dp = 3.dp
    /** Menus, pressed cards. */
    val level3: Dp = 6.dp
    /** Dialogs, bottom sheets, FABs. */
    val level4: Dp = 8.dp
}

/**
 * Sizing constants that are accessibility requirements rather than taste.
 * [minTouchTarget] is the Material and WCAG floor — anything tappable that ends up
 * smaller than this needs a `Modifier.sizeIn` or a larger hit area, not a smaller icon.
 */
object Sizing {
    val minTouchTarget: Dp = 48.dp
    val iconSm: Dp = 16.dp
    val iconMd: Dp = 20.dp
    val iconLg: Dp = 24.dp
    val avatar: Dp = 40.dp
    val avatarLg: Dp = 56.dp
    /** Square behind a stat tile's icon. */
    val iconTile: Dp = 40.dp
    /** Hairline used for card outlines; 1dp reads as heavy on xxhdpi. */
    val hairline: Dp = 1.dp
}

internal val LocalSpacing = staticCompositionLocalOf { Spacing.Compact }

/** `MaterialTheme.spacing.md` — mirrors how you already reach for `MaterialTheme.colorScheme`. */
val androidx.compose.material3.MaterialTheme.spacing: Spacing
    @Composable @ReadOnlyComposable get() = LocalSpacing.current

package com.greenwood.school.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * Material 3 scheme built from the web app's MUI palette.
 *
 * ## On dynamic colour (Material You)
 *
 * Dynamic colour is wired up but **off by default**, and that is a product
 * decision rather than an oversight. Greenwood ships a web client and an Android
 * client that users move between in the same session; if the phone recolours the
 * app to match the wallpaper, the two stop looking like one product and the
 * indigo/amber identity survives only on devices whose wallpaper happens to be
 * blue. Pass `dynamicColor = true` to [SchoolTheme] — or flip [DYNAMIC_COLOR_DEFAULT]
 * — to turn it on globally once that trade-off is acceptable. Everything downstream
 * reads its colours from `MaterialTheme.colorScheme`, so nothing else has to change.
 *
 * Dynamic colour needs Android 12 (API 31); on anything older the brand scheme is
 * used regardless of the flag.
 */
const val DYNAMIC_COLOR_DEFAULT: Boolean = false

private val LightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = White,
    primaryContainer = Color_E8EAF6,
    onPrimaryContainer = BrandIndigoDark,
    inversePrimary = BrandIndigoLight,

    secondary = BrandAmber,
    onSecondary = BrandIndigoDark,
    secondaryContainer = Color_FFF3D6,
    onSecondaryContainer = Color_6B4A00,

    tertiary = StatusInfo,
    onTertiary = White,
    tertiaryContainer = Color(0xFFD6EEFA),
    onTertiaryContainer = Color(0xFF00405C),

    error = StatusError,
    onError = White,
    errorContainer = Color_FDECEA,
    onErrorContainer = Color_7A1A15,

    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfacePaperLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceContainerHighLight,
    onSurfaceVariant = TextSecondaryLight,

    // Tonal depth ladder — see the note in Color.kt.
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,

    outline = Color_C7CBD8,
    outlineVariant = DividerLight,
    inverseSurface = BrandIndigoDark,
    inverseOnSurface = SidebarContent,
    scrim = ScrimBlack,
)

private val DarkColors = darkColorScheme(
    primary = BrandIndigoLight,
    onPrimary = White,
    primaryContainer = BrandIndigoDark,
    onPrimaryContainer = SidebarContent,
    inversePrimary = BrandIndigo,

    secondary = BrandAmber,
    onSecondary = BrandIndigoDark,
    secondaryContainer = BrandAmberDark,
    onSecondaryContainer = White,

    tertiary = StatusInfo,
    onTertiary = White,
    tertiaryContainer = Color(0xFF00405C),
    onTertiaryContainer = Color(0xFFD6EEFA),

    error = Color_F28B82,
    onError = Color_3B0906,
    errorContainer = Color_5C1712,
    onErrorContainer = Color_FDECEA,

    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfacePaperDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color_1D2437,
    onSurfaceVariant = TextSecondaryDark,

    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,

    outline = Color_3A4258,
    outlineVariant = DividerDark,
    inverseSurface = SidebarContent,
    inverseOnSurface = BrandIndigoDark,
    scrim = ScrimBlack,
)

/**
 * Corner radii. The brief asks for a consistent 12-16dp family, so [Shapes.medium] —
 * what cards, dialogs and sheets resolve to — is 16dp and everything else steps
 * around it.
 *
 * Note that M3 buttons are pill-shaped by default and do **not** read from here;
 * that is intentional and left alone.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // text fields, small chips
    small = RoundedCornerShape(12.dp),       // chips, icon tiles
    medium = RoundedCornerShape(16.dp),      // cards, dialogs — the default
    large = RoundedCornerShape(20.dp),       // hero cards, bottom sheets
    extraLarge = RoundedCornerShape(28.dp),  // full-screen sheets
)

/**
 * Colours Material's own scheme has no role for, but that more than one screen
 * needs to agree on. Kept small on purpose — anything that *can* be expressed as a
 * standard M3 role belongs in the scheme above, not here.
 */
@Immutable
data class AppExtraColors(
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val onHero: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
)

private val LocalExtraColors = staticCompositionLocalOf {
    AppExtraColors(
        heroGradientStart = HeroGradientStartLight,
        heroGradientEnd = HeroGradientEndLight,
        onHero = White,
        shimmerBase = ShimmerBaseLight,
        shimmerHighlight = ShimmerHighlightLight,
    )
}

/** `MaterialTheme.extraColors.shimmerBase` — same access shape as `colorScheme`. */
val MaterialTheme.extraColors: AppExtraColors
    @Composable @ReadOnlyComposable get() = LocalExtraColors.current

/**
 * When dynamic colour is on, the extras have to follow the wallpaper too, otherwise
 * a Material You device gets a brand-indigo hero bar floating above a lilac app.
 * Deriving them from the resolved scheme keeps the two in step either way.
 */
private fun extraColorsFor(
    scheme: ColorScheme,
    darkTheme: Boolean,
    brandScheme: Boolean,
): AppExtraColors = AppExtraColors(
    heroGradientStart = when {
        !brandScheme -> scheme.primary
        darkTheme -> HeroGradientStartDark
        else -> HeroGradientStartLight
    },
    heroGradientEnd = when {
        !brandScheme -> scheme.tertiary
        darkTheme -> HeroGradientEndDark
        else -> HeroGradientEndLight
    },
    onHero = if (brandScheme) White else scheme.onPrimary,
    shimmerBase = if (darkTheme) ShimmerBaseDark else ShimmerBaseLight,
    shimmerHighlight = if (darkTheme) ShimmerHighlightDark else ShimmerHighlightLight,
)

@Composable
fun SchoolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** See the note at the top of this file before turning this on. */
    dynamicColor: Boolean = DYNAMIC_COLOR_DEFAULT,
    /** Swap in [Spacing.Expanded] from a window-size-class check to loosen the whole grid. */
    spacing: Spacing = Spacing.Compact,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Icon tint is what actually matters here; from API 35 the bar colours
            // themselves are ignored in favour of edge-to-edge.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalExtraColors provides extraColorsFor(colorScheme, darkTheme, brandScheme = !useDynamic),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

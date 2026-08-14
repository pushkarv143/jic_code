package com.greenwood.school.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * Material 3 scheme built from the web app's MUI palette.
 *
 * Dynamic colour (Material You) is deliberately **not** used: this is a branded
 * school product and the indigo/amber identity must survive on every device.
 */
private val LightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = White,
    primaryContainer = Color_E8EAF6,
    onPrimaryContainer = BrandIndigoDark,
    secondary = BrandAmber,
    onSecondary = BrandIndigoDark,
    secondaryContainer = Color_FFF3D6,
    onSecondaryContainer = Color_6B4A00,
    tertiary = StatusInfo,
    onTertiary = White,
    error = StatusError,
    onError = White,
    errorContainer = Color_FDECEA,
    onErrorContainer = Color_7A1A15,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfacePaperLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color_C7CBD8,
    outlineVariant = DividerLight,
    inverseSurface = BrandIndigoDark,
    inverseOnSurface = SidebarContent,
)

private val DarkColors = darkColorScheme(
    primary = BrandIndigoLight,
    onPrimary = White,
    primaryContainer = BrandIndigoDark,
    onPrimaryContainer = SidebarContent,
    secondary = BrandAmber,
    onSecondary = BrandIndigoDark,
    secondaryContainer = BrandAmberDark,
    onSecondaryContainer = White,
    tertiary = StatusInfo,
    onTertiary = White,
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
    outline = Color_3A4258,
    outlineVariant = DividerDark,
    inverseSurface = SidebarContent,
    inverseOnSurface = BrandIndigoDark,
)

/** MUI `shape.borderRadius: 10`, with cards at 14. */
private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
)

@Composable
fun SchoolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

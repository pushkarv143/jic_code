package com.greenwood.school.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * The full Material 3 type scale — display, headline, title, body, label.
 *
 * The previous version stopped at `headlineLarge`, so anything that wanted to be
 * genuinely big (a dashboard hero figure, a collapsed-toolbar title) had to invent
 * its own `fontSize`. Every tier now exists, which is what makes "no magic font
 * sizes in screen code" enforceable rather than aspirational.
 *
 * Sizes still track the web theme's MUI scale so the two clients read as one product.
 *
 * The web app asks for Inter and falls back to Roboto; on Android the system font
 * *is* Roboto, so we use the platform default with the same weights, sizes and
 * letter-spacing rather than shipping a ~300KB font binary. Drop Inter into
 * `res/font/` and swap [AppFontFamily] if exact parity is ever required.
 */
private val AppFontFamily = FontFamily.Default

/**
 * Trims the extra leading Android puts above the first line and below the last.
 * Without this, a headline in a Card looks mis-centred no matter how the padding
 * is tuned — the text box is taller than the glyphs.
 */
private val TrimmedLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun appStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = AppFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = TrimmedLineHeight,
)

val AppTypography = Typography(
    /* ---- Display: reserved for single hero numbers and empty-state headlines. ---- */
    displayLarge = appStyle(48, 56, FontWeight.Bold, -1.0),
    displayMedium = appStyle(40, 48, FontWeight.Bold, -0.8),
    displaySmall = appStyle(32, 40, FontWeight.Bold, -0.5),

    /* ---- Headline: page and expanded-toolbar titles. ---- */
    headlineLarge = appStyle(28, 36, FontWeight.Bold, -0.3),
    headlineMedium = appStyle(24, 32, FontWeight.Bold, -0.2),
    headlineSmall = appStyle(20, 28, FontWeight.SemiBold),

    /* ---- Title: card headers and collapsed toolbars. ---- */
    titleLarge = appStyle(18, 24, FontWeight.SemiBold),
    titleMedium = appStyle(16, 24, FontWeight.SemiBold, 0.1),
    titleSmall = appStyle(14, 20, FontWeight.SemiBold, 0.1),

    /* ---- Body: prose and list content. ---- */
    bodyLarge = appStyle(16, 24, FontWeight.Normal, 0.15),
    bodyMedium = appStyle(14, 20, FontWeight.Normal, 0.15),
    bodySmall = appStyle(12, 16, FontWeight.Normal, 0.2),

    /* ---- Label: buttons, chips, captions, overlines. MUI `button` is 600 and not
            uppercased, so labels here are SemiBold with normal casing. ---- */
    labelLarge = appStyle(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = appStyle(12, 16, FontWeight.SemiBold, 0.4),
    labelSmall = appStyle(11, 16, FontWeight.Medium, 0.5),
)

/**
 * Numeric styles for figures that must not reflow as digits change.
 * Currency and percentages in the stat tiles use these so a value ticking from
 * ₹9,999 to ₹10,000 doesn't shift the label underneath it.
 */
object NumericType {
    /** The big figure on a stat tile. */
    val statValue = appStyle(24, 30, FontWeight.Bold, -0.4)

    /** A stat tile's supporting caption. */
    val statCaption = appStyle(11, 16, FontWeight.Medium, 0.3)
}

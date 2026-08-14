package com.greenwood.school.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette lifted directly from `school-frontend/src/theme/theme.ts` so the two
 * clients are visually the same product.
 *
 * MUI derives its own light/dark tints from `main`; Material 3 wants explicit
 * container/on-colour pairs, so the derived values below were chosen to match
 * what MUI actually renders rather than being generated from scratch.
 */

/* Brand — MUI `PRIMARY` */
val BrandIndigo = Color(0xFF2B3A8F)
val BrandIndigoLight = Color(0xFF5563B8)
val BrandIndigoDark = Color(0xFF1C2763)

/* Brand — MUI `SECONDARY` */
val BrandAmber = Color(0xFFFFB703)
val BrandAmberLight = Color(0xFFFFCB47)
val BrandAmberDark = Color(0xFFC88900)

/* Semantic — MUI success/warning/error/info */
val StatusSuccess = Color(0xFF2E7D32)
val StatusWarning = Color(0xFFED6C02)
val StatusError = Color(0xFFD32F2F)
val StatusInfo = Color(0xFF0288D1)

/* Surfaces — MUI `background.default` / `background.paper` */
val SurfaceLight = Color(0xFFF4F6FB)
val SurfacePaperLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF0F1420)
val SurfacePaperDark = Color(0xFF161D2E)

/* Text — MUI `text.primary` / `text.secondary` */
val TextPrimaryLight = Color(0xFF1A1F36)
val TextSecondaryLight = Color(0xFF5A6072)
val TextPrimaryDark = Color(0xFFE6E9F2)
val TextSecondaryDark = Color(0xFF9AA2B8)

/* Dividers — MUI `divider` */
val DividerLight = Color(0x171A1F36)
val DividerDark = Color(0x17E6E9F2)

/* Sidebar / navigation drawer — MUI custom `palette.sidebar` */
val SidebarLight = Color(0xFF1C2763)
val SidebarDark = Color(0xFF0B0F1C)
val SidebarContent = Color(0xFFE6E9F2)

val White = Color(0xFFFFFFFF)

/*
 * Container / outline tones Material 3 needs but MUI has no direct equivalent for.
 * Each is a tint or shade of a brand colour above, picked to match what MUI's own
 * `alpha()` helpers render for the same role on the web.
 */
val Color_E8EAF6 = Color(0xFFE8EAF6) // primaryContainer  — indigo 50
val Color_FFF3D6 = Color(0xFFFFF3D6) // secondaryContainer — amber 50
val Color_6B4A00 = Color(0xFF6B4A00) // onSecondaryContainer
val Color_FDECEA = Color(0xFFFDECEA) // errorContainer
val Color_7A1A15 = Color(0xFF7A1A15) // onErrorContainer
val Color_C7CBD8 = Color(0xFFC7CBD8) // outline (light)
val Color_1D2437 = Color(0xFF1D2437) // surfaceVariant (dark)
val Color_3A4258 = Color(0xFF3A4258) // outline (dark)
val Color_F28B82 = Color(0xFFF28B82) // error (dark)
val Color_3B0906 = Color(0xFF3B0906) // onError (dark)
val Color_5C1712 = Color(0xFF5C1712) // errorContainer (dark)

/**
 * Status colours used on chips across attendance, fees, leave, payroll and
 * library. Centralised so "PAID" is the same green everywhere it appears.
 */
object StatusPalette {
    val positive = StatusSuccess
    val negative = StatusError
    val caution = StatusWarning
    val neutral = Color(0xFF5A6072)
    val info = StatusInfo

    /** Maps any backend enum value onto the four chip colours. */
    fun forStatus(status: String?): Color = when (status?.uppercase()) {
        "PRESENT", "PAID", "APPROVED", "ACTIVE", "RETURNED", "GRADED", "SENT" -> positive
        "ABSENT", "REJECTED", "OVERDUE", "TERMINATED", "FAILED", "UNPAID" -> negative
        "LATE", "PARTIAL", "PENDING", "HALF_DAY", "RESIGNED", "SUBMITTED" -> caution
        "LEAVE", "ISSUED", "TRANSFERRED", "ALUMNI", "VACATED" -> info
        else -> neutral
    }
}

package com.greenwood.school.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Calendar

/** The attribution line itself, kept in one place so every surface agrees. */
const val COPYRIGHT_TEXT = "All rights reserved by Pushkar Verma"

/**
 * Site-wide attribution footer, the Android counterpart of
 * `school-frontend/src/components/common/AppFooter.tsx`.
 *
 * Hosted once at the nav-host level rather than per screen, so it covers the signed-out
 * auth screens and the signed-in shell alike — including any screen added later, which
 * would otherwise have to remember to include it.
 *
 * Kept deliberately compact: it occupies every screen permanently, so the vertical space
 * it costs is paid on all of them.
 */
@Composable
fun AppFooter(modifier: Modifier = Modifier) {
    val year = Calendar.getInstance().get(Calendar.YEAR)

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "© $year $COPYRIGHT_TEXT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

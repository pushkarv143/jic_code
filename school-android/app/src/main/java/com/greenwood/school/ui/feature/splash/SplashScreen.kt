package com.greenwood.school.ui.feature.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greenwood.school.R
import com.greenwood.school.ui.theme.BrandAmber
import com.greenwood.school.ui.theme.BrandIndigo
import com.greenwood.school.ui.theme.BrandIndigoDark
import com.greenwood.school.ui.theme.BrandIndigoLight

const val DEVELOPER_CREDIT = "Developed by Pushkar Verma"

/**
 * Launch screen with the same indigo gradient as LoginScreen, so the transition
 * from splash → login feels like one continuous flow.
 * Minimum display time is controlled by MainActivity (now 600 ms).
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 480),
        label = "splashFade",
    )

    // Suppress the unused variable warning — kept for future shimmer use.
    @Suppress("UNUSED_VARIABLE")
    val infiniteTransition = rememberInfiniteTransition(label = "loader")
    val progressAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "progress",
    )

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrandIndigoDark, BrandIndigo, BrandIndigoLight.copy(alpha = 0.9f)),
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        // Decorative amber glow, top-right
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BrandAmber.copy(alpha = 0.12f), Color.Transparent),
                    )
                )
                .align(Alignment.TopEnd)
        )
        // Lighter indigo glow, bottom-left
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BrandIndigoLight.copy(alpha = 0.22f), Color.Transparent),
                    )
                )
                .align(Alignment.BottomStart)
        )

        // Centre content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(contentAlpha)
                .padding(horizontal = 40.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = BrandAmber,
                    modifier = Modifier.size(50.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.school_name),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "School Management System",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.62f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(100.dp)),
                color = BrandAmber,
                trackColor = Color.White.copy(alpha = 0.18f),
                strokeCap = StrokeCap.Round,
            )
        }

        // Developer credit, bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(4.dp).clip(CircleShape).background(BrandAmber.copy(alpha = 0.55f)))
            Spacer(Modifier.width(7.dp))
            Text(
                text = DEVELOPER_CREDIT,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.5f),
            )
            Spacer(Modifier.width(7.dp))
            Box(Modifier.size(4.dp).clip(CircleShape).background(BrandAmber.copy(alpha = 0.55f)))
        }
    }
}

package com.greenwood.school.ui.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenwood.school.R
import com.greenwood.school.core.network.AppError
import com.greenwood.school.ui.components.AppTextField
import com.greenwood.school.ui.components.PasswordField
import com.greenwood.school.ui.theme.BrandAmber
import com.greenwood.school.ui.theme.BrandIndigo
import com.greenwood.school.ui.theme.BrandIndigoDark
import com.greenwood.school.ui.theme.BrandIndigoLight

@Composable
fun LoginScreen(
    onSignedIn: (mustChangePassword: Boolean) -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToOtpLogin: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn(state.mustChangePassword)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrandIndigoDark,
                        BrandIndigo,
                        BrandIndigoLight.copy(alpha = 0.85f),
                    )
                )
            )
    ) {
        // Decorative blobs
        Box(
            modifier = Modifier
                .size(320.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandAmber.copy(alpha = 0.12f),
                            Color.Transparent,
                        )
                    )
                )
                .align(Alignment.TopEnd)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandIndigoLight.copy(alpha = 0.3f),
                            Color.Transparent,
                        )
                    )
                )
                .align(Alignment.BottomStart)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Logo badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = BrandAmber,
                        modifier = Modifier.size(40.dp),
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.school_name),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.sign_in_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(32.dp))

                // Form card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                        Text(
                            text = "Welcome back",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Sign in to continue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(24.dp))

                        AppTextField(
                            value = state.username,
                            onValueChange = viewModel::onUsernameChange,
                            label = stringResource(R.string.username),
                            error = state.usernameError,
                            enabled = !state.isSubmitting,
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                            modifier = Modifier.testTag(TAG_USERNAME),
                        )

                        Spacer(Modifier.height(14.dp))

                        PasswordField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = stringResource(R.string.password),
                            error = state.passwordError,
                            required = false,
                            modifier = Modifier.testTag(TAG_PASSWORD),
                        )

                        if (state.formError != null) {
                            Spacer(Modifier.height(14.dp))
                            LoginErrorBanner(state.formError!!)
                        }

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = viewModel::submit,
                            enabled = !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag(TAG_SUBMIT),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandIndigo,
                            ),
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Text(
                                    stringResource(R.string.sign_in),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onNavigateToOtpLogin, enabled = !state.isSubmitting) {
                        Text(
                            stringResource(R.string.sign_in_with_code),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    TextButton(onClick = onNavigateToForgotPassword, enabled = !state.isSubmitting) {
                        Text(
                            stringResource(R.string.forgot_password),
                            color = BrandAmber,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.accounts_created_by_school),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    } 
}

@Composable
private fun LoginErrorBanner(error: AppError) {
    val isAccountIssue = error is AppError.Forbidden
    val bgColor = if (isAccountIssue) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
    val fgColor = if (isAccountIssue) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = fgColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(10.dp))
            Text(error.userMessage, style = MaterialTheme.typography.bodySmall, color = fgColor)
        }
    }
}

internal const val TAG_USERNAME = "login_username"
internal const val TAG_PASSWORD = "login_password"
internal const val TAG_SUBMIT = "login_submit"

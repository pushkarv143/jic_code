package com.greenwood.school.ui.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenwood.school.R
import com.greenwood.school.core.network.AppError
import com.greenwood.school.ui.components.AppTextField
import com.greenwood.school.ui.components.PasswordField

/**
 * Sign-in, mirroring `school-frontend/src/pages/auth/LoginPage.tsx`: same brand
 * mark, same wording, same two fields, same links out to registration and password
 * recovery.
 */
@Composable
fun LoginScreen(
    /**
     * Called once the credentials are accepted. The flag says whether the account is
     * still on a school-generated password, which decides whether the dashboard or
     * the change-password screen comes next.
     */
    onSignedIn: (mustChangePassword: Boolean) -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    /** Passwordless sign-in — a code emailed to the address on the account. */
    onNavigateToOtpLogin: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn(state.mustChangePassword)
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_school_logo),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.school_name),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.sign_in_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                AppTextField(
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange,
                    label = stringResource(R.string.username),
                    error = state.usernameError,
                    enabled = !state.isSubmitting,
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    modifier = Modifier.testTag(TAG_USERNAME),
                )

                Spacer(Modifier.height(12.dp))

                PasswordField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = stringResource(R.string.password),
                    error = state.passwordError,
                    required = false,
                    modifier = Modifier.testTag(TAG_PASSWORD),
                )

                if (state.formError != null) {
                    Spacer(Modifier.height(12.dp))
                    LoginErrorBanner(state.formError!!)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = viewModel::submit,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag(TAG_SUBMIT),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.sign_in))
                    }
                }

                Spacer(Modifier.height(4.dp))

                TextButton(onClick = onNavigateToOtpLogin, enabled = !state.isSubmitting) {
                    Text(stringResource(R.string.sign_in_with_code))
                }

                TextButton(onClick = onNavigateToForgotPassword, enabled = !state.isSubmitting) {
                    Text(stringResource(R.string.forgot_password))
                }

                Spacer(Modifier.height(8.dp))

                // No "create an account" here any more. The school admits students
                // through its own form, which generates a username and a first-time
                // password and emails them, so there is nothing for a student to
                // sign up for — and an account that could exist before the admission
                // did was one nobody had asked for.
                Text(
                    text = stringResource(R.string.accounts_created_by_school),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

/**
 * The backend distinguishes "wrong password" (401) from "account disabled/locked/
 * expired" (403) and writes the exact sentence to show. Rendering the server's own
 * message keeps Android and web identical; only the icon tone differs by severity.
 */
@Composable
private fun LoginErrorBanner(error: AppError) {
    val isAccountIssue = error is AppError.Forbidden
    Surface(
        color = if (isAccountIssue) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        contentColor = if (isAccountIssue) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(10.dp))
            Text(error.userMessage, style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal const val TAG_USERNAME = "login_username"
internal const val TAG_PASSWORD = "login_password"
internal const val TAG_SUBMIT = "login_submit"

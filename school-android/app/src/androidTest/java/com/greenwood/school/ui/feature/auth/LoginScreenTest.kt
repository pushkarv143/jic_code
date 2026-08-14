package com.greenwood.school.ui.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.data.remote.dto.UserDto
import com.greenwood.school.domain.repository.AuthRepository
import com.greenwood.school.ui.theme.SchoolTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Covers the login flow end to end at the UI layer, with a fake repository so the
 * test never touches the network.
 */
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun submittingEmptyFormShowsBothFieldErrors() {
        composeRule.setContent {
            SchoolTheme {
                LoginScreen(
                    onSignedIn = {},
                    onNavigateToRegister = {},
                    onNavigateToForgotPassword = {},
                    viewModel = LoginViewModel(StubAuthRepository()),
                )
            }
        }

        composeRule.onNodeWithTag(TAG_SUBMIT).performClick()

        composeRule.onNodeWithText("Username is required").assertIsDisplayed()
        composeRule.onNodeWithText("Password is required").assertIsDisplayed()
    }

    @Test
    fun validCredentialsInvokeTheSignedInCallback() {
        var signedIn = false
        composeRule.setContent {
            SchoolTheme {
                LoginScreen(
                    onSignedIn = { signedIn = true },
                    onNavigateToRegister = {},
                    onNavigateToForgotPassword = {},
                    viewModel = LoginViewModel(StubAuthRepository()),
                )
            }
        }

        composeRule.onNodeWithTag(TAG_USERNAME).performTextInput("admin")
        composeRule.onNodeWithTag(TAG_PASSWORD).performTextInput("Admin@123")
        composeRule.onNodeWithTag(TAG_SUBMIT).performClick()
        composeRule.waitForIdle()

        assertTrue(signedIn)
    }

    @Test
    fun aRejectedLoginShowsTheServerMessage() {
        val repository = StubAuthRepository(
            result = ApiResult.Failure(AppError.Unauthorized("Invalid username or password")),
        )
        composeRule.setContent {
            SchoolTheme {
                LoginScreen(
                    onSignedIn = {},
                    onNavigateToRegister = {},
                    onNavigateToForgotPassword = {},
                    viewModel = LoginViewModel(repository),
                )
            }
        }

        composeRule.onNodeWithTag(TAG_USERNAME).performTextInput("admin")
        composeRule.onNodeWithTag(TAG_PASSWORD).performTextInput("nope")
        composeRule.onNodeWithTag(TAG_SUBMIT).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Invalid username or password").assertIsDisplayed()
    }
}

private class StubAuthRepository(
    private val result: ApiResult<UserDto> = ApiResult.Success(
        UserDto(id = 1, username = "admin", role = "SUPER_ADMIN"),
    ),
) : AuthRepository {

    override val currentUser: StateFlow<UserDto?> = MutableStateFlow(null)

    override suspend fun login(username: String, password: String) = result
    override suspend fun logout() = ApiResult.Success(Unit)
    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        username: String,
        password: String,
        role: String,
    ) = ApiResult.Success(Unit)

    override suspend fun forgotPassword(email: String) = ApiResult.Success(Unit)
    override suspend fun resetPassword(token: String, newPassword: String) = ApiResult.Success(Unit)
    override suspend fun changePassword(currentPassword: String, newPassword: String) = ApiResult.Success(Unit)
    override suspend fun refreshProfile() = result
}

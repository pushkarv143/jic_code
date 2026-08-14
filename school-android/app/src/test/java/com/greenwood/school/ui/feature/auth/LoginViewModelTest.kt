package com.greenwood.school.ui.feature.auth

import app.cash.turbine.test
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.data.remote.dto.UserDto
import com.greenwood.school.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        viewModel = LoginViewModel(repository)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `blank fields are rejected without calling the API`() = runTest(dispatcher) {
        viewModel.submit()

        val state = viewModel.state.value
        assertEquals("Username is required", state.usernameError)
        assertEquals("Password is required", state.passwordError)
        assertEquals(0, repository.loginCallCount)
    }

    @Test
    fun `a successful login flips isSignedIn and stops the spinner`() = runTest(dispatcher) {
        repository.loginResult = ApiResult.Success(sampleUser)
        viewModel.onUsernameChange("admin")
        viewModel.onPasswordChange("Admin@123")

        viewModel.state.test {
            awaitItem() // initial

            viewModel.submit()
            assertTrue(awaitItem().isSubmitting)

            val done = awaitItem()
            assertTrue(done.isSignedIn)
            assertFalse(done.isSubmitting)
            assertNull(done.formError)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, repository.loginCallCount)
    }

    @Test
    fun `a disabled account surfaces the servers 403 message verbatim`() = runTest(dispatcher) {
        val message = "Your account is not active. Please contact the school administrator."
        repository.loginResult = ApiResult.Failure(AppError.Forbidden(message))
        viewModel.onUsernameChange("student1")
        viewModel.onPasswordChange("Password@123")

        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isSignedIn)
        assertFalse(state.isSubmitting)
        assertEquals(message, state.formError?.userMessage)
    }

    @Test
    fun `typing clears a previous failure so the banner does not linger`() = runTest(dispatcher) {
        repository.loginResult = ApiResult.Failure(AppError.Unauthorized("Invalid username or password"))
        viewModel.onUsernameChange("admin")
        viewModel.onPasswordChange("wrong")
        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.formError != null)

        viewModel.onPasswordChange("Admin@123")

        assertNull(viewModel.state.value.formError)
    }

    @Test
    fun `a second submit while in flight is ignored`() = runTest(dispatcher) {
        repository.loginResult = ApiResult.Success(sampleUser)
        viewModel.onUsernameChange("admin")
        viewModel.onPasswordChange("Admin@123")

        viewModel.submit()
        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.loginCallCount)
    }

    private val sampleUser = UserDto(id = 1, username = "admin", role = "SUPER_ADMIN")
}

/**
 * Hand-written fake rather than a mock: the ViewModel only needs two behaviours
 * from the repository, and a fake makes the call count assertions obvious.
 */
private class FakeAuthRepository : AuthRepository {

    var loginResult: ApiResult<UserDto> = ApiResult.Success(UserDto(1, "admin", role = "SUPER_ADMIN"))
    var loginCallCount = 0

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    override val currentUser: StateFlow<UserDto?> = _currentUser

    override suspend fun login(username: String, password: String): ApiResult<UserDto> {
        loginCallCount++
        return loginResult.also { if (it is ApiResult.Success) _currentUser.value = it.data }
    }

    override suspend fun logout(): ApiResult<Unit> {
        _currentUser.value = null
        return ApiResult.Success(Unit)
    }

    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        username: String,
        password: String,
        role: String,
    ): ApiResult<Unit> = ApiResult.Success(Unit)

    override suspend fun forgotPassword(email: String): ApiResult<Unit> = ApiResult.Success(Unit)

    override suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit> =
        ApiResult.Success(Unit)

    override suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> =
        ApiResult.Success(Unit)

    override suspend fun refreshProfile(): ApiResult<UserDto> = loginResult
}

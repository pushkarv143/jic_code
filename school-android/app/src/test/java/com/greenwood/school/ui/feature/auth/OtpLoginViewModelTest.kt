package com.greenwood.school.ui.feature.auth

import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.data.remote.dto.JwtAuthResponseDto
import com.greenwood.school.data.remote.dto.OtpPurpose
import com.greenwood.school.data.remote.dto.OtpSendResponseDto
import com.greenwood.school.data.remote.dto.OtpVerifyResponseDto
import com.greenwood.school.data.remote.dto.UserDto
import com.greenwood.school.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Passwordless sign-in.
 *
 * Uses a phone-shaped destination throughout: the email branch of the validator
 * needs `android.util.Patterns`, which the JVM suite does not have.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OtpLoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var auth: AuthRepository
    private lateinit var vm: OtpLoginViewModel

    private val destination = "9810011122"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        auth = mockk(relaxed = true)
        coEvery { auth.requestOtp(any(), any()) } returns ApiResult.Success(OtpSendResponseDto())
        vm = OtpLoginViewModel(auth)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the code is requested for LOGIN, not for a password reset`() = runTest(dispatcher) {
        vm.onDestinationChange(destination)
        vm.sendCode()
        advanceUntilIdle()

        // Purposes are not interchangeable server-side: a reset code presented here
        // would simply not be found.
        coVerify(exactly = 1) { auth.requestOtp(destination, OtpPurpose.LOGIN) }
        assertTrue(vm.state.value.codeSent)
    }

    @Test
    fun `a correct code signs the user in`() = runTest(dispatcher) {
        coEvery { auth.verifyOtp(destination, OtpPurpose.LOGIN, "482915") } returns
            ApiResult.Success(OtpVerifyResponseDto(auth = session()))

        vm.onDestinationChange(destination)
        vm.onCodeChange("482915")
        vm.signIn()
        advanceUntilIdle()

        // The repository stores the session, so the screen only has to move on.
        assertTrue(vm.state.value.isSignedIn)
    }

    @Test
    fun `a rejected code leaves the user signed out and says so on the field`() = runTest(dispatcher) {
        coEvery { auth.verifyOtp(any(), any(), any()) } returns
            ApiResult.Failure(AppError.Validation("That code is incorrect or has expired."))

        vm.onDestinationChange(destination)
        vm.onCodeChange("000000")
        vm.signIn()
        advanceUntilIdle()

        assertFalse(vm.state.value.isSignedIn)
        assertEquals("That code is incorrect or has expired.", vm.state.value.codeError)
    }

    @Test
    fun `a deactivated account is surfaced rather than signing anyone in`() = runTest(dispatcher) {
        // The server refuses this on the login call itself; the screen must not
        // treat a failure as anything other than a failure.
        coEvery { auth.verifyOtp(any(), any(), any()) } returns
            ApiResult.Failure(AppError.Forbidden("Your account is not active."))

        vm.onDestinationChange(destination)
        vm.onCodeChange("482915")
        vm.signIn()
        advanceUntilIdle()

        assertFalse(vm.state.value.isSignedIn)
        assertNotNull(vm.state.value.codeError)
    }

    @Test
    fun `a short code never costs one of the five server-side attempts`() = runTest(dispatcher) {
        vm.onDestinationChange(destination)
        vm.onCodeChange("48")
        vm.signIn()
        advanceUntilIdle()

        assertNotNull(vm.state.value.codeError)
        coVerify(exactly = 0) { auth.verifyOtp(any(), any(), any()) }
    }

    private fun session() = JwtAuthResponseDto(
        accessToken = "access",
        refreshToken = "refresh",
        user = UserDto(id = 1, username = "student1", role = "STUDENT", firstName = "Ravi"),
    )
}

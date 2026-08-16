package com.greenwood.school.ui.feature.auth

import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.data.remote.dto.OtpPurpose
import com.greenwood.school.data.remote.dto.OtpSendResponseDto
import com.greenwood.school.data.remote.dto.OtpVerifyResponseDto
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
 * The three-step recovery flow.
 *
 * Every case uses a phone-shaped destination rather than an email: the email
 * branch of the validator calls `android.util.Patterns`, which the JVM suite does
 * not have. The step logic under test is the same either way — only which
 * validator runs differs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var auth: AuthRepository
    private lateinit var vm: ForgotPasswordViewModel

    private val destination = "9810011122"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        auth = mockk(relaxed = true)
        coEvery { auth.requestOtp(any(), any()) } returns ApiResult.Success(OtpSendResponseDto())
        vm = ForgotPasswordViewModel(auth)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a blank destination never reaches the API`() = runTest(dispatcher) {
        vm.sendCode()
        advanceUntilIdle()

        assertNotNull(vm.state.value.destinationError)
        coVerify(exactly = 0) { auth.requestOtp(any(), any()) }
    }

    @Test
    fun `sending a code opens step two and starts the resend countdown`() = runTest(dispatcher) {
        coEvery { auth.requestOtp(destination, OtpPurpose.PASSWORD_RESET) } returns
            ApiResult.Success(OtpSendResponseDto(expiresInSeconds = 300, resendAfterSeconds = 60))

        vm.onDestinationChange(destination)
        vm.sendCode()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.codeSent)
        assertEquals(60, state.resendInSeconds)
        // The wording must not confirm the account exists, because the server does not.
        assertEquals("If an account matches, a code has been sent.", state.message)
    }

    @Test
    fun `the code field keeps only digits and stops at six`() {
        vm.onCodeChange("4a8b29-15999")

        assertEquals("482915", vm.state.value.code)
    }

    @Test
    fun `a short code is rejected before it costs an attempt`() = runTest(dispatcher) {
        vm.onDestinationChange(destination)
        vm.onCodeChange("4829")
        vm.verifyCode()
        advanceUntilIdle()

        assertNotNull(vm.state.value.codeError)
        // The server allows only five guesses, so spending one on a malformed
        // code would be throwing an attempt away.
        coVerify(exactly = 0) { auth.verifyOtp(any(), any(), any()) }
    }

    @Test
    fun `a correct code opens step three and keeps the reset token`() = runTest(dispatcher) {
        coEvery { auth.verifyOtp(destination, OtpPurpose.PASSWORD_RESET, "482915") } returns
            ApiResult.Success(OtpVerifyResponseDto(resetToken = "reset-token-abc"))

        vm.onDestinationChange(destination)
        vm.onCodeChange("482915")
        vm.verifyCode()
        advanceUntilIdle()

        assertTrue(vm.state.value.codeVerified)
        assertEquals("reset-token-abc", vm.state.value.resetToken)
    }

    @Test
    fun `a wrong code is reported on the field, not as a passing message`() = runTest(dispatcher) {
        coEvery { auth.verifyOtp(any(), any(), any()) } returns
            ApiResult.Failure(AppError.Validation("That code is incorrect or has expired."))

        vm.onDestinationChange(destination)
        vm.onCodeChange("000000")
        vm.verifyCode()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.codeVerified)
        // Against the field, because the code is what they must retype.
        assertEquals("That code is incorrect or has expired.", state.codeError)
    }

    @Test
    fun `mismatched passwords never reach the API`() = runTest(dispatcher) {
        vm.onNewPasswordChange("Password@123")
        vm.onConfirmPasswordChange("Password@124")
        vm.resetPassword()
        advanceUntilIdle()

        assertNotNull(vm.state.value.confirmPasswordError)
        coVerify(exactly = 0) { auth.resetPassword(any(), any()) }
    }

    @Test
    fun `the whole flow ends by spending the token and clearing it`() = runTest(dispatcher) {
        coEvery { auth.verifyOtp(any(), any(), any()) } returns
            ApiResult.Success(OtpVerifyResponseDto(resetToken = "reset-token-abc"))
        coEvery { auth.resetPassword("reset-token-abc", "Password@123") } returns ApiResult.Success(Unit)

        vm.onDestinationChange(destination)
        vm.sendCode()
        advanceUntilIdle()
        vm.onCodeChange("482915")
        vm.verifyCode()
        advanceUntilIdle()
        vm.onNewPasswordChange("Password@123")
        vm.onConfirmPasswordChange("Password@123")
        vm.resetPassword()
        advanceUntilIdle()

        assertTrue(vm.state.value.isComplete)
        // Not kept around after it has been spent.
        assertEquals("", vm.state.value.resetToken)
        coVerify(exactly = 1) { auth.resetPassword("reset-token-abc", "Password@123") }
    }
}

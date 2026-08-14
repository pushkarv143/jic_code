package com.greenwood.school.core.network

import android.content.Context
import com.greenwood.school.R
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * The error contract is the part of the app users notice most, so it is tested
 * against the exact bodies `GlobalExceptionHandler` produces.
 */
class ErrorMapperTest {

    private lateinit var mapper: ErrorMapper

    @Before
    fun setUp() {
        val context = mockk<Context>(relaxed = true)
        every { context.getString(R.string.error_network) } returns "No internet connection."
        every { context.getString(R.string.error_timeout) } returns "The request timed out. Please try again."
        every { context.getString(R.string.error_unauthorized) } returns "Your session has expired."
        every { context.getString(R.string.error_forbidden) } returns "You do not have permission."
        every { context.getString(R.string.error_not_found) } returns "We couldn't find that."
        every { context.getString(R.string.error_conflict) } returns "That record already exists."
        every { context.getString(R.string.error_validation) } returns "Please check the highlighted fields."
        every { context.getString(R.string.error_server) } returns "Something went wrong on our end."
        every { context.getString(R.string.error_unknown) } returns "Something went wrong."

        mapper = ErrorMapper(context, Json { ignoreUnknownKeys = true })
    }

    @Test
    fun `400 with validationErrors becomes a Validation error carrying every field`() {
        val body = """
            {
              "timestamp": "2026-08-13T10:00:00",
              "status": 400,
              "error": "Bad Request",
              "message": "Validation failed",
              "path": "/api/v1/students",
              "validationErrors": { "rollNumber": "Roll number is required", "gender": "must not be null" }
            }
        """.trimIndent()

        val error = mapper.mapHttpStatus(400, body)

        assertTrue(error is AppError.Validation)
        error as AppError.Validation
        assertEquals("Validation failed", error.userMessage)
        assertEquals(2, error.fieldErrors.size)
        assertEquals("Roll number is required", error.fieldErrors["rollNumber"])
    }

    @Test
    fun `403 keeps the servers own account-status wording`() {
        // The backend writes a specific sentence per DisabledException / LockedException;
        // the app must show that, not a generic "forbidden".
        val body = """
            {"status":403,"error":"Forbidden",
             "message":"Your account is not active. Please contact the school administrator.",
             "path":"/api/v1/auth/login"}
        """.trimIndent()

        val error = mapper.mapHttpStatus(403, body)

        assertTrue(error is AppError.Forbidden)
        assertEquals(
            "Your account is not active. Please contact the school administrator.",
            error.userMessage,
        )
    }

    @Test
    fun `401 maps to Unauthorized and is flagged as an auth expiry`() {
        val error = mapper.mapHttpStatus(401, """{"status":401,"message":"JWT token has expired"}""")

        assertTrue(error is AppError.Unauthorized)
        assertTrue(error.isAuthExpiry)
    }

    @Test
    fun `409 maps to Conflict`() {
        val error = mapper.mapHttpStatus(409, """{"status":409,"message":"Admission number already exists"}""")

        assertTrue(error is AppError.Conflict)
        assertEquals("Admission number already exists", error.userMessage)
    }

    @Test
    fun `500 never leaks the server message to the user`() {
        val body = """{"status":500,"message":"NullPointerException at com.school.sms.Foo:42"}"""

        val error = mapper.mapHttpStatus(500, body)

        assertTrue(error is AppError.Server)
        assertEquals("Something went wrong on our end.", error.userMessage)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `422 is treated as validation, matching how the backend uses it`() {
        val error = mapper.mapHttpStatus(422, """{"status":422,"message":"Fee already fully paid"}""")

        assertTrue(error is AppError.Validation)
        assertEquals("Fee already fully paid", error.userMessage)
    }

    @Test
    fun `an unparseable error body still yields a usable message`() {
        val error = mapper.mapHttpStatus(404, "<html>404 Not Found</html>")

        assertTrue(error is AppError.NotFound)
        assertEquals("We couldn't find that.", error.userMessage)
    }

    @Test
    fun `network and timeout exceptions are distinguished`() {
        assertTrue(mapper.map(UnknownHostException("no dns")) is AppError.Network)
        assertTrue(mapper.map(IOException("socket closed")) is AppError.Network)
        assertTrue(mapper.map(SocketTimeoutException("timeout")) is AppError.Timeout)
    }

    @Test
    fun `an AppException passes its error through untouched`() {
        val original = AppError.Conflict("Duplicate")

        assertEquals(original, mapper.map(AppException(original)))
    }

    @Test
    fun `only network, timeout and server failures are retryable`() {
        assertTrue(AppError.Network("x").isRetryable)
        assertTrue(AppError.Timeout("x").isRetryable)
        assertTrue(AppError.Server("x", 500).isRetryable)
        assertTrue(!AppError.Forbidden("x").isRetryable)
        assertTrue(!AppError.Validation("x").isRetryable)
    }
}

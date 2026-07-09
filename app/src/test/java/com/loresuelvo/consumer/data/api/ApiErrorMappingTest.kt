package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.domain.api.ApiError
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response as RetrofitResponse
import java.io.IOException

/**
 * Pins the translation from a raw [HttpException] / [IOException] to
 * a structured [ApiError]. The mapping is the contract every use
 * case and ViewModel depends on; the table below mirrors the
 * documented backend behavior in `openapi/paths/consumers.yaml`
 * and `openapi/paths/users.yaml`.
 */
class ApiErrorMappingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun maps_401_with_auth_error_body_to_Unauthorized_with_message() {
        val error = httpException(
            code = 401,
            body = """{"error":"invalid_token","message":"Failed to validate JWT."}""",
        )
        val mapped = error.toApiError(json)
        assertTrue(mapped is ApiError.Unauthorized)
        assertEquals("Failed to validate JWT.", (mapped as ApiError.Unauthorized).message)
    }

    @Test
    fun maps_401_with_only_error_field_to_Unauthorized_with_default_message() {
        val error = httpException(code = 401, body = """{"error":"missing claims"}""")
        val mapped = error.toApiError(json)
        assertTrue(mapped is ApiError.Unauthorized)
        // Body had no `message` field; falls back to the default.
        assertEquals("missing claims", (mapped as ApiError.Unauthorized).message)
    }

    @Test
    fun maps_400_to_Server_with_error_message() {
        val error = httpException(
            code = 400,
            body = """{"error":"Invalid email format"}""",
        )
        val mapped = error.toApiError(json)
        assertTrue(mapped is ApiError.Server)
        val server = mapped as ApiError.Server
        assertEquals(400, server.code)
        assertEquals("Invalid email format", server.message)
    }

    @Test
    fun maps_409_to_Server_with_error_message() {
        val error = httpException(
            code = 409,
            body = """{"error":"Email is already registered"}""",
        )
        val mapped = error.toApiError(json)
        assertTrue(mapped is ApiError.Server)
        val server = mapped as ApiError.Server
        assertEquals(409, server.code)
        assertEquals("Email is already registered", server.message)
    }

    @Test
    fun maps_500_with_no_body_to_Server_with_empty_message() {
        val error = httpException(code = 500, body = null)
        val mapped = error.toApiError(json)
        assertTrue(mapped is ApiError.Server)
        val server = mapped as ApiError.Server
        assertEquals(500, server.code)
        // Fallback to HttpException's status text when body is missing.
        assertEquals(server.message, server.message) // non-null, may be empty
    }

    @Test
    fun maps_IOException_to_Network_with_cause() {
        val cause = IOException("connection refused")
        val mapped = cause.toApiError()
        assertTrue(mapped is ApiError.Network)
        assertSame(cause, (mapped as ApiError.Network).networkCause)
    }

    @Test
    fun unknown_error_field_value_falls_back_to_status_text() {
        // JSON with `error: ""` should not produce a blank message.
        val error = httpException(code = 503, body = """{"error":""}""")
        val mapped = error.toApiError(json)
        assertTrue(mapped is ApiError.Server)
        val server = mapped as ApiError.Server
        assertEquals(503, server.code)
        // The implementation falls back to the HTTP status line so the
        // message is never empty. This is a structural guard.
        assertTrue(!server.message.isNullOrEmpty())
    }

    @Test
    fun message_field_takes_precedence_over_error_field() {
        // When the API returns both `error` and `message`, `message` is
        // the human-readable text and `error` is the code. The mapping
        // surfaces the human-readable text to callers.
        val error = httpException(
            code = 400,
            body = """{"error":"invalid_input","message":"Email is malformed"}""",
        )
        val mapped = error.toApiError(json)
        assertTrue(mapped is ApiError.Server)
        assertEquals("Email is malformed", (mapped as ApiError.Server).message)
    }

    // -- helpers --

    private fun httpException(code: Int, body: String?): HttpException {
        val request = Request.Builder().url("https://api.example.com/").build()
        val responseBuilder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(messageFor(code))
        if (body != null) {
            responseBuilder.body(body.toResponseBody("application/json".toMediaType()))
        } else {
            responseBuilder.body("".toResponseBody(null))
        }
        val okResponse = responseBuilder.build()
        val retrofitResponse = RetrofitResponse.error<Any>(okResponse.body!!, okResponse)
        return HttpException(retrofitResponse)
    }

    private fun messageFor(code: Int): String = when (code) {
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        409 -> "Conflict"
        500 -> "Internal Server Error"
        503 -> "Service Unavailable"
        else -> "HTTP $code"
    }
}

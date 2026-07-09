package com.loresuelvo.consumer.data.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies the documented policy of [RetryOn401Authenticator]: the
 * backend does not expose a refresh-token endpoint, so we never retry
 * on 401. Auth0's SDK handles token refresh internally; the resulting
 * 401 that reaches the OkHttp authenticator must propagate to the
 * caller so the use case can clear the local session.
 *
 * If the API ever ships a refresh endpoint, this test will need to
 * change to assert a retry is performed (and the authenticator
 * implementation will need to call that endpoint).
 */
class RetryOn401AuthenticatorTest {

    private val authenticator = RetryOn401Authenticator()

    @Test
    fun returns_null_on_first_401_so_caller_clears_session() {
        val original = Request.Builder()
            .url("https://api.example.com/consumers")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        val response = Response.Builder()
            .request(original)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody(null))
            .build()

        val result = authenticator.authenticate(null, response)
        assertNull("authenticator must not retry without a refresh policy", result)
    }

    @Test
    fun returns_null_on_repeated_401_no_retry_loop() {
        // Sanity: even if the response is the same, no retry is attempted.
        val original = Request.Builder()
            .url("https://api.example.com/me")
            .get()
            .build()
        val response = Response.Builder()
            .request(original)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody(null))
            .build()

        val first = authenticator.authenticate(null, response)
        val second = authenticator.authenticate(null, response)
        assertNull(first)
        assertNull(second)
    }
}

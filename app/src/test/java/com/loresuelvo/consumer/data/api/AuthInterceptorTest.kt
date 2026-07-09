package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies the [AuthInterceptor] adds `Authorization: Bearer <token>`
 * when a session exists, and leaves the request unchanged when it
 * does not. The interceptor is the single source of truth for token
 * injection; everything else relies on the headers it sets.
 */
class AuthInterceptorTest {

    private val authSessionStore = mockk<AuthSessionStore>()
    private val chain = mockk<Interceptor.Chain>()
    private val interceptor = AuthInterceptor(authSessionStore)

    private fun responseFor(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("".toResponseBody(null))
        .build()

    @Test
    fun adds_bearer_token_when_session_is_present() {
        val original = Request.Builder()
            .url("https://api.example.com/consumers")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        val passedThrough = original.newBuilder().build()
        every { chain.request() } returns passedThrough
        every { chain.proceed(any()) } returns responseFor(passedThrough)
        every { authSessionStore.getSession() } returns AuthSession(
            user = User(displayName = "Ana"),
            accessToken = "jwt-token-123",
        )

        interceptor.intercept(chain)

        val captor = slot<Request>()
        verify { chain.proceed(capture(captor)) }
        assertEquals("Bearer jwt-token-123", captor.captured.header("Authorization"))
    }

    @Test
    fun does_not_add_header_when_no_session() {
        val original = Request.Builder()
            .url("https://api.example.com/me")
            .get()
            .build()
        every { chain.request() } returns original
        every { chain.proceed(any()) } returns responseFor(original)
        every { authSessionStore.getSession() } returns null

        interceptor.intercept(chain)

        val captor = slot<Request>()
        verify { chain.proceed(capture(captor)) }
        assertNull(captor.captured.header("Authorization"))
    }

    @Test
    fun does_not_add_header_when_token_is_blank() {
        val original = Request.Builder()
            .url("https://api.example.com/me")
            .get()
            .build()
        every { chain.request() } returns original
        every { chain.proceed(any()) } returns responseFor(original)
        every { authSessionStore.getSession() } returns AuthSession(
            user = User(displayName = "Ana"),
            accessToken = "",
        )

        interceptor.intercept(chain)

        val captor = slot<Request>()
        verify { chain.proceed(capture(captor)) }
        assertNull(captor.captured.header("Authorization"))
    }

    @Test
    fun returns_chain_response() {
        val original = Request.Builder()
            .url("https://api.example.com/me")
            .get()
            .build()
        val expected = responseFor(original)
        every { chain.request() } returns original
        every { chain.proceed(any()) } returns expected
        every { authSessionStore.getSession() } returns null

        val result = interceptor.intercept(chain)
        assertEquals(expected, result)
    }
}

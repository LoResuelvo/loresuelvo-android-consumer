package com.loresuelvo.consumer.domain.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for the [ApiError] sealed hierarchy. These tests pin the
 * public shape of the type so future refactors don't accidentally
 * break the mapping from HTTP failures to domain failures.
 */
class ApiErrorTest {

    @Test
    fun network_carries_original_cause() {
        val cause = IOException("connection refused")
        val error: ApiError = ApiError.Network(cause)
        assertTrue("error should be ApiError.Network", error is ApiError.Network)
        val network = error as ApiError.Network
        assertSame(cause, network.networkCause)
        assertEquals("Network error", network.message)
    }

    @Test
    fun server_carries_status_code_and_message() {
        val error: ApiError = ApiError.Server(code = 409, errorMessage = "Email is already registered")
        assertTrue("error should be ApiError.Server", error is ApiError.Server)
        val server = error as ApiError.Server
        assertEquals(409, server.code)
        assertEquals("Email is already registered", server.message)
    }

    @Test
    fun unauthorized_has_default_message() {
        val error: ApiError = ApiError.Unauthorized()
        assertTrue("error should be ApiError.Unauthorized", error is ApiError.Unauthorized)
        val unauthorized = error as ApiError.Unauthorized
        assertEquals("Unauthorized", unauthorized.message)
    }

    @Test
    fun unauthorized_accepts_custom_message() {
        val error: ApiError = ApiError.Unauthorized("Token expired")
        val unauthorized = error as ApiError.Unauthorized
        assertEquals("Token expired", unauthorized.message)
    }

    @Test
    fun unknown_can_carry_a_cause() {
        val cause = RuntimeException("kaboom")
        val error: ApiError = ApiError.Unknown(cause)
        assertTrue("error should be ApiError.Unknown", error is ApiError.Unknown)
        val unknown = error as ApiError.Unknown
        assertSame(cause, unknown.unknownCause)
        assertEquals("Unknown error", unknown.message)
    }

    @Test
    fun unknown_works_without_a_cause() {
        val error: ApiError = ApiError.Unknown()
        val unknown = error as ApiError.Unknown
        assertEquals("Unknown error", unknown.message)
    }
}

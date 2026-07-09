package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies [ApiErrorDto] against every documented error response in
 * the OpenAPI spec.
 *
 * - `error-response.yaml`: required `error: string`, no `message`.
 * - `auth-error-response.yaml`: required `error: enum`, optional
 *   `message: string`.
 *
 * `message` is parsed as nullable in this DTO because the
 * `error-response` schema does not include it; `additionalProperties:
 * false` means anything else is rejected by the backend. We accept
 * it as a no-op when present.
 */
class ApiErrorDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun parses_simple_error_response() {
        val payload = """{"error":"Invalid email format"}"""
        val decoded = json.decodeFromString(ApiErrorDto.serializer(), payload)
        assertEquals("Invalid email format", decoded.error)
        assertNull(decoded.message)
    }

    @Test
    fun parses_auth_error_response_with_message() {
        val payload = """{"error":"invalid_token","message":"Failed to validate JWT."}"""
        val decoded = json.decodeFromString(ApiErrorDto.serializer(), payload)
        assertEquals("invalid_token", decoded.error)
        assertEquals("Failed to validate JWT.", decoded.message)
    }

    @Test
    fun parses_auth_error_response_without_message() {
        val payload = """{"error":"missing claims"}"""
        val decoded = json.decodeFromString(ApiErrorDto.serializer(), payload)
        assertEquals("missing claims", decoded.error)
        assertNull(decoded.message)
    }

    @Test
    fun parses_email_already_registered_409() {
        val payload = """{"error":"Email is already registered"}"""
        val decoded = json.decodeFromString(ApiErrorDto.serializer(), payload)
        assertEquals("Email is already registered", decoded.error)
    }

    @Test
    fun parses_user_not_found_404() {
        val payload = """{"error":"User was not found"}"""
        val decoded = json.decodeFromString(ApiErrorDto.serializer(), payload)
        assertEquals("User was not found", decoded.error)
    }
}

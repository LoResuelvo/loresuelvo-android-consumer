package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies that [MessageResponseDto] matches the exact JSON the
 * backend returns for the `POST /consumers` 201 success and the
 * other generic `message`-only responses used across the API.
 *
 * Spec: `openapi/components/schemas/message-response.yaml`
 *   required: [message]
 *   additionalProperties: false
 */
class MessageResponseDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun deserializes_real_consumer_registration_success() {
        val payload = """{"message":"cuenta registrada exitosamente"}"""
        val decoded = json.decodeFromString(MessageResponseDto.serializer(), payload)
        assertEquals("cuenta registrada exitosamente", decoded.message)
    }

    @Test
    fun round_trip_preserves_message() {
        val original = MessageResponseDto("hello world")
        val encoded = json.encodeToString(MessageResponseDto.serializer(), original)
        val decoded = json.decodeFromString(MessageResponseDto.serializer(), encoded)
        assertEquals(original, decoded)
    }
}

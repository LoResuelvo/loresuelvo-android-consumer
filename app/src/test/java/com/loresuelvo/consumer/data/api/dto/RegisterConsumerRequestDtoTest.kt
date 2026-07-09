package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that [RegisterConsumerRequestDto] serializes to the exact
 * snake_case JSON shape the backend expects. The backend OpenAPI
 * spec (`openapi/components/schemas/register-consumer-request.yaml`)
 * requires `email`, `name`, `surname`; the DTO uses camelCase
 * internally with `@SerialName` annotations, so the wire format
 * must be correct or the API will reject the request with 400.
 */
class RegisterConsumerRequestDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun serializes_to_snake_case_field_names() {
        val dto = RegisterConsumerRequestDto(
            email = "ana@example.com",
            firstName = "Ana",
            surname = "Perez",
        )
        val encoded = json.encodeToString(RegisterConsumerRequestDto.serializer(), dto)
        assertEquals(
            """{"email":"ana@example.com","name":"Ana","surname":"Perez"}""",
            encoded,
        )
    }

    @Test
    fun deserializes_from_snake_case_payload() {
        val payload = """{"email":"a@x.com","name":"A","surname":"B"}"""
        val decoded = json.decodeFromString(RegisterConsumerRequestDto.serializer(), payload)
        assertEquals("a@x.com", decoded.email)
        assertEquals("A", decoded.firstName)
        assertEquals("B", decoded.surname)
    }

    @Test
    fun round_trip_preserves_values() {
        val original = RegisterConsumerRequestDto(
            email = "x@y.com",
            firstName = "X",
            surname = "Y",
        )
        val encoded = json.encodeToString(RegisterConsumerRequestDto.serializer(), original)
        val decoded = json.decodeFromString(RegisterConsumerRequestDto.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun accepts_payload_with_extra_unknown_keys() {
        val payload = """{"email":"a@x.com","name":"A","surname":"B","future_field":"ignored"}"""
        val decoded = json.decodeFromString(RegisterConsumerRequestDto.serializer(), payload)
        assertEquals("a@x.com", decoded.email)
        assertTrue(decoded.firstName.isNotEmpty())
    }
}

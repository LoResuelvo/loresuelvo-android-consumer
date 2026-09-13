package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.RegisterConsumerRequestDto
import com.loresuelvo.consumer.data.api.dto.RegisterConsumerAddressDto
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The mapper is the only place where the camelCase domain model
 * meets the snake_case wire format. This test pins the field-by-
 * field translation; if any field drifts, the backend will reject
 * the request with 400.
 */
class UserDtoMapperTest {

    @Test
    fun maps_domain_data_to_dto_with_correct_field_names() {
        val data = RegisterConsumerData(
            email = "ana@example.com",
            firstName = "Ana",
            lastName = "Perez",
            address = RegisterConsumerAddress(
                street = "Tucuman",
                streetNumber = "123",
                floor = "1",
                unit = "A",
            )
        )
        val dto = data.toDto()
        assertEquals("ana@example.com", dto.email)
        // `firstName` in the domain maps to `name` on the wire.
        assertEquals("Ana", dto.firstName)
        // `lastName` in the domain maps to `surname` on the wire.
        assertEquals("Perez", dto.surname)
    }

    @Test
    fun preserves_empty_strings() {
        val data = RegisterConsumerData(
            email = "a@x.com",
            firstName = "",
            lastName = "",
            address = RegisterConsumerAddress(
                street = "",
                streetNumber = "",
                floor = "",
                unit = "",
            )
        )
        val dto = data.toDto()
        assertEquals("", dto.firstName)
        assertEquals("", dto.surname)
        // We do NOT call TrimSpace here; the backend trims on its
        // side. Sending the raw value keeps the contract simple.
    }

    @Test
    fun dto_carries_serial_name_annotations() {
        // Compile-time check: the generated @SerialName annotations on
        // the DTO must be 'email', 'name', 'surname'. We assert by
        // serializing and checking the exact JSON keys.
        val dto = RegisterConsumerRequestDto(
            email = "x",
            firstName = "X",
            surname = "Y",
            address = RegisterConsumerAddressDto(
                street = "S",
                streetNumber = "N",
                floor = "F",
                unit = "U",
            )
        )
        val json = kotlinx.serialization.json.Json
        val encoded = json.encodeToString(RegisterConsumerRequestDto.serializer(), dto)
        assertEquals(
            """{"email":"x","name":"X","surname":"Y","address":{"street":"S","street_number":"N","floor":"F","unit":"U"}}""",
            encoded,
        )
    }
}

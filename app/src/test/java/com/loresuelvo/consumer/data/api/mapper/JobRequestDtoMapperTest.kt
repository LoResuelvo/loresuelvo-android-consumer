package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.CreateJobRequestDto
import com.loresuelvo.consumer.data.api.dto.JobRequestDto
import com.loresuelvo.consumer.data.api.dto.JobRequestImageDto
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the DTO ↔ domain translation for the `POST /job-requests`
 * endpoint. The wire shape (snake_case + `Long` ids + nested
 * `images`) is captured on 2026-07-28 from the swagger contract
 * the user shared.
 */
class JobRequestDtoMapperTest {

    @Test
    fun maps_response_dto_to_domain_with_typed_ids_and_images() {
        val dto = JobRequestDto(
            id = 1L,
            conversationId = 10L,
            title = "Reparación de fuga en la cocina",
            description = "Hola Ana, necesito reparar una fuga de agua en la cocina.",
            status = "pending",
            images = listOf(
                JobRequestImageDto(
                    id = "4af47f1b-97b6-4b32-baa0-b95d6077f919",
                    url = "https://cdn.example/private/job-request-image.jpg?signature=temporary",
                    originalName = "perdida-bajo-mesada.webp",
                ),
            ),
        )

        val jobRequest = dto.toDomain()

        assertEquals("1", jobRequest.id)
        assertEquals("10", jobRequest.conversationId)
        assertEquals("Reparación de fuga en la cocina", jobRequest.title)
        assertEquals(
            "Hola Ana, necesito reparar una fuga de agua en la cocina.",
            jobRequest.description,
        )
        assertEquals("pending", jobRequest.status)
        assertEquals(1, jobRequest.images.size)
        assertEquals(
            "4af47f1b-97b6-4b32-baa0-b95d6077f919",
            jobRequest.images[0].id,
        )
        assertEquals("perdida-bajo-mesada.webp", jobRequest.images[0].originalName)
    }

    @Test
    fun null_conversation_id_collapses_to_null_domain_conversationId() {
        val dto = JobRequestDto(
            id = 1L,
            conversationId = null,
            title = "x",
            description = "y",
            status = "pending",
            images = null,
        )

        val jobRequest = dto.toDomain()

        assertNull(jobRequest.conversationId)
    }

    @Test
    fun null_images_collapses_to_empty_list_in_domain() {
        val dto = JobRequestDto(
            id = 1L,
            conversationId = 1L,
            title = "x",
            description = "y",
            status = "pending",
            images = null,
        )

        val jobRequest = dto.toDomain()

        assertEquals(emptyList<Any>(), jobRequest.images)
    }

    @Test
    fun request_data_to_dto_carries_optional_image_file_ids() {
        val data = CreateJobRequestData(
            providerId = 1,
            title = "Fuga",
            description = "Hay una fuga",
        )

        val dto = data.toDto()

        assertEquals(1, dto.providerId)
        assertEquals("Fuga", dto.title)
        assertEquals("Hay una fuga", dto.description)
        // Empty list drops to `null` so kotlinx-serialization omits
        // the key from the JSON payload (the backend treats the
        // field as optional).
        assertNull(dto.imageFileIds)
    }

    @Test
    fun request_data_to_dto_carries_image_file_ids_when_present() {
        val data = CreateJobRequestData(
            providerId = 1,
            title = "Fuga",
            description = "Hay una fuga",
            imageFileIds = listOf("img-1", "img-2"),
        )

        val dto = data.toDto()

        assertEquals(listOf("img-1", "img-2"), dto.imageFileIds)
    }
}

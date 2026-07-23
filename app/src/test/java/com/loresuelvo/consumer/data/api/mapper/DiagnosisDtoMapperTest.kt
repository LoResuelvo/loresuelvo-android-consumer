package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ChatMessageDto
import com.loresuelvo.consumer.data.api.dto.DiagnosisDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the DTO → domain translation for the AI diagnostic chat.
 * The wire shape uses snake_case (`sender_role`, `sent_at`,
 * `conversation_id`); the domain uses camelCase. Each test pins
 * one mapping decision so a backend shape drift is caught
 * immediately.
 */
class DiagnosisDtoMapperTest {

    @Test
    fun maps_id_field_to_conversationId() {
        // The backend returns the conversation id as a number
        // (`"id": 1`). The mapper keeps the domain's `String`
        // representation so `LazyColumn` keys don't overflow.
        val dto = DiagnosisDto(
            id = 42L,
            messages = emptyList(),
        )

        val diagnosis = dto.toDomain()

        assertEquals("42", diagnosis.conversationId)
    }

    @Test
    fun maps_conversation_id_field_as_fallback_for_conversationId() {
        // Some backend revisions emit `conversation_id` (not `id`)
        // on the create-and-send path. The mapper tolerates both.
        val dto = DiagnosisDto(
            id = null,
            conversationId = "conv-99",
            messages = emptyList(),
        )

        val diagnosis = dto.toDomain()

        assertEquals("conv-99", diagnosis.conversationId)
    }

    @Test
    fun maps_sender_role_consumer_to_Sender_Consumer() {
        val dto = ChatMessageDto(
            id = 1L,
            senderRole = "consumer",
            content = "Tengo una gotera",
        )

        val message = dto.toDomain()

        assertTrue(message.sender == com.loresuelvo.consumer.domain.diagnosis.Sender.Consumer)
    }

    @Test
    fun maps_sender_role_chatbot_to_Sender_Assistant() {
        val dto = ChatMessageDto(
            id = 2L,
            senderRole = "chatbot",
            content = "¿Es constante?",
        )

        val message = dto.toDomain()

        assertTrue(message.sender == com.loresuelvo.consumer.domain.diagnosis.Sender.Assistant)
    }

    @Test
    fun unknown_sender_role_defaults_to_Sender_Assistant_defensively() {
        // Future-proofing: if the backend introduces a third role
        // (e.g. "system"), we still want the bubble to render on
        // the left. Logging falls on the eventual usage site.
        val dto = ChatMessageDto(
            id = 3L,
            senderRole = "system",
            content = "internal note",
        )

        val message = dto.toDomain()

        assertTrue(message.sender == com.loresuelvo.consumer.domain.diagnosis.Sender.Assistant)
    }

    @Test
    fun maps_empty_messages_list_to_empty_list() {
        val dto = DiagnosisDto(id = 42L, messages = emptyList())

        val diagnosis = dto.toDomain()

        assertEquals(0, diagnosis.messages.size)
    }

    @Test
    fun maps_messages_in_order_preserving_field_projection() {
        val dto = DiagnosisDto(
            id = 42L,
            messages = listOf(
                ChatMessageDto(id = 1L, senderRole = "consumer", content = "primera"),
                ChatMessageDto(id = 2L, senderRole = "chatbot", content = "primera respuesta"),
                ChatMessageDto(id = 3L, senderRole = "consumer", content = "segunda"),
                ChatMessageDto(id = 4L, senderRole = "chatbot", content = "segunda respuesta"),
            ),
        )

        val diagnosis = dto.toDomain()

        assertEquals(listOf("1", "2", "3", "4"), diagnosis.messages.map { it.id })
        assertEquals(
            listOf("primera", "primera respuesta", "segunda", "segunda respuesta"),
            diagnosis.messages.map { it.content },
        )
        assertEquals(
            listOf(
                com.loresuelvo.consumer.domain.diagnosis.Sender.Consumer,
                com.loresuelvo.consumer.domain.diagnosis.Sender.Assistant,
                com.loresuelvo.consumer.domain.diagnosis.Sender.Consumer,
                com.loresuelvo.consumer.domain.diagnosis.Sender.Assistant,
            ),
            diagnosis.messages.map { it.sender },
        )
    }

    @Test
    fun parses_iso_sentAt_to_epoch_millis_when_present() {
        // YYYY-MM-DDTHH:MM:SS, parsed as UTC. Using the epoch
        // reference (1970-01-01T00:00:01Z -> 1000L) avoids any
        // JDK timezone drift the test could mask.
        val dto = ChatMessageDto(
            id = 1L,
            senderRole = "consumer",
            content = "x",
            sentAt = "1970-01-01T00:00:01",
        )

        val message = dto.toDomain()

        assertEquals(1000L, message.sentAtEpochMillis)
    }

    @Test
    fun falls_back_to_createdOn_when_sentAt_is_null() {
        val dto = ChatMessageDto(
            id = 1L,
            senderRole = "consumer",
            content = "x",
            sentAt = null,
            createdOn = "2025-06-15T12:30:00",
        )

        val message = dto.toDomain()

        // 2025-06-15T12:30:00Z -> 1750002600000L (sanity)
        assertTrue(message.sentAtEpochMillis != 0L)
    }

    @Test
    fun returns_zero_epoch_when_both_timestamps_are_null() {
        val dto = ChatMessageDto(
            id = 1L,
            senderRole = "consumer",
            content = "x",
            sentAt = null,
            createdOn = null,
        )

        val message = dto.toDomain()

        assertEquals(0L, message.sentAtEpochMillis)
    }

    @Test
    fun mapper_propagates_recommendations_as_null_until_later_commits() {
        // The mapper currently can't decode `assessment` /
        // `recommended_providers`. Reserve `null` for now — 09-DIA
        // will adjust this assertion.
        val dto = DiagnosisDto(
            id = 42L,
            messages = listOf(
                ChatMessageDto(id = 1L, senderRole = "chatbot", content = "diagnóstico listo"),
            ),
        )

        val diagnosis = dto.toDomain()

        assertNull(diagnosis.recommendations)
    }
}

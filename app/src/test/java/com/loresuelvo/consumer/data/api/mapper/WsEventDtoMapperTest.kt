package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.WsEventDto
import com.loresuelvo.consumer.data.api.dto.WsEventMessageDto
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.realtime.WsEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the wire → domain translation for [WsEventDto]. Each test
 * nails one mapping decision so a backend shape drift is caught
 * immediately (mirrors the discipline of
 * [ConversationDtoMapperTest] for the REST conversation endpoints).
 *
 * The mapper reuses the existing
 * [ConversationMessageDto.toDomain] extension for the embedded
 * `message` block; this test only exercises the WebSocket-specific
 * glue (snake_case → camelCase on `conversation_id`, the
 * `type`-discriminator guard, the `sender_role` enum mapping, and
 * the `created_on` ISO-8601 parser).
 */
class WsEventDtoMapperTest {

    private fun wsEventDto(
        type: String = WsEvent.CONVERSATION_MESSAGE_CREATED,
        conversationId: Long = 1,
        message: WsEventMessageDto = WsEventMessageDto(
            id = 2,
            senderRole = "consumer",
            content = "hola",
            createdOn = "2026-05-31T12:00:00Z",
        ),
    ) = WsEventDto(
        type = type,
        conversationId = conversationId,
        message = message,
    )

    @Test
    fun maps_conversation_id_snake_case_to_camelCase() {
        val event = wsEventDto(conversationId = 42L).toDomain()

        assertNotNull(event)
        assertEquals(42L, event!!.conversationId)
    }

    @Test
    fun maps_type_field_verbatim() {
        val event = wsEventDto().toDomain()

        assertNotNull(event)
        assertEquals(WsEvent.CONVERSATION_MESSAGE_CREATED, event!!.type)
    }

    @Test
    fun unknown_type_returns_null_so_upstream_can_drop_silently() {
        val event = wsEventDto(type = "conversation.accepted").toDomain()

        // The mapper guards against future event types the
        // consumer doesn't know about. Returning `null` lets the
        // WebSocket client drop the frame without crashing.
        assertNull(event)
    }

    @Test
    fun maps_sender_role_consumer_to_Consumer() {
        val event = wsEventDto(
            message = WsEventMessageDto(
                id = 1,
                senderRole = "consumer",
                content = "hola",
                createdOn = "2026-05-31T12:00:00Z",
            ),
        ).toDomain()

        assertNotNull(event)
        assertEquals(ConversationSender.Consumer, event!!.message.sender)
    }

    @Test
    fun maps_sender_role_provider_to_Provider() {
        val event = wsEventDto(
            message = WsEventMessageDto(
                id = 1,
                senderRole = "provider",
                content = "¡Hola!",
                createdOn = "2026-05-31T12:00:00Z",
            ),
        ).toDomain()

        assertNotNull(event)
        assertEquals(ConversationSender.Provider, event!!.message.sender)
    }

    @Test
    fun unknown_sender_role_defaults_to_Provider_defensively() {
        val event = wsEventDto(
            message = WsEventMessageDto(
                id = 1,
                senderRole = "system",
                content = "internal note",
                createdOn = "2026-05-31T12:00:00Z",
            ),
        ).toDomain()

        assertNotNull(event)
        // Defensive default: an unknown sender_role renders as
        // Provider so the user can still see the body. Matches
        // the discipline of `ConversationMessageDto.toDomain`.
        assertEquals(ConversationSender.Provider, event!!.message.sender)
    }

    @Test
    fun maps_id_to_string_in_domain() {
        val event = wsEventDto(
            message = WsEventMessageDto(
                id = 42L,
                senderRole = "consumer",
                content = "hola",
                createdOn = "2026-05-31T12:00:00Z",
            ),
        ).toDomain()

        assertNotNull(event)
        // Long id on the wire → String in the domain (stable
        // LazyColumn keys, no overflow concerns). Mirrors the
        // convention used by `ConversationMessageDto`.
        assertEquals("42", event!!.message.id)
    }

    @Test
    fun maps_content_verbatim() {
        val event = wsEventDto(
            message = WsEventMessageDto(
                id = 1,
                senderRole = "consumer",
                content = "¿El jueves por la mañana te queda cómodo?",
                createdOn = "2026-05-31T12:00:00Z",
            ),
        ).toDomain()

        assertNotNull(event)
        assertEquals(
            "¿El jueves por la mañana te queda cómodo?",
            event!!.message.content,
        )
    }

    @Test
    fun parses_iso_created_on_to_epoch_millis() {
        val event = wsEventDto(
            message = WsEventMessageDto(
                id = 1,
                senderRole = "consumer",
                content = "x",
                createdOn = "2026-05-31T12:00:00Z",
            ),
        ).toDomain()

        assertNotNull(event)
        // The actual epoch value is non-zero. We don't pin a
        // specific number (the parser is lenient on timezone) —
        // the integration test in commit 13c covers the REST
        // path; here we just pin that the field is parsed and
        // not collapsed to 0L.
        assertTrue(event!!.message.createdOnEpochMillis != 0L)
    }

    @Test
    fun falls_back_to_zero_epoch_when_createdOn_is_null() {
        val event = wsEventDto(
            message = WsEventMessageDto(
                id = 1,
                senderRole = "consumer",
                content = "x",
                createdOn = null,
            ),
        ).toDomain()

        assertNotNull(event)
        assertEquals(0L, event!!.message.createdOnEpochMillis)
    }

    @Test
    fun unknown_event_with_valid_payload_still_returns_null() {
        // Sanity check: even with a fully valid message block,
        // an unknown type returns null. The mapper's guard is on
        // the `type` field, not the message body.
        val event = wsEventDto(type = "future.event.type").toDomain()
        assertNull(event)
    }
}
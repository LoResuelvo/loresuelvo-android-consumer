package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ConversationMessageDto
import com.loresuelvo.consumer.data.api.dto.MessageVideoDto
import com.loresuelvo.consumer.data.api.dto.WsEventDto
import com.loresuelvo.consumer.data.api.dto.WsEventMessageDto
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.realtime.WsEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationVideoMapperTest {

    private val video = MessageVideoDto(
        id = "video-1",
        url = "https://cdn.example/video-1.mp4",
        originalName = "evidence.mp4",
        mimeType = "video/mp4",
        videoCodec = "h264",
        audioCodec = "aac",
        durationSeconds = 20,
        width = 1280,
        height = 720,
    )

    @Test
    fun rest_message_maps_video_without_converting_it_to_image() {
        val message = ConversationMessageDto(
            id = 7,
            senderRole = "provider",
            content = "Mirá este detalle",
            video = video,
        ).toDomain()

        assertTrue(message.media is MediaReference.Video)
        val mapped = message.media as MediaReference.Video
        assertEquals("video-1", mapped.id)
        assertEquals("evidence.mp4", mapped.originalName)
        assertEquals(20_000L, mapped.durationMillis)
        assertEquals(1280, mapped.width)
        assertEquals(720, mapped.height)
        assertEquals("h264", mapped.videoCodec)
        assertEquals("aac", mapped.audioCodec)
    }

    @Test
    fun websocket_message_maps_video_with_the_same_domain_shape() {
        val event = WsEventDto(
            type = WsEvent.CONVERSATION_MESSAGE_CREATED,
            conversationId = 1,
            message = WsEventMessageDto(
                id = 8,
                senderRole = "provider",
                content = "video",
                video = video,
            ),
        ).toDomain()

        assertTrue(event != null)
        val messageEvent = event as WsEvent.ConversationMessageCreated
        assertTrue(messageEvent.message.media is MediaReference.Video)
        assertEquals("video-1", (messageEvent.message.media as MediaReference.Video).id)
    }
}

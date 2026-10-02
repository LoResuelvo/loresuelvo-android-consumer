package com.loresuelvo.consumer.domain.usecase.conversation

import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.MAX_AUDIO_BYTES
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [SendMediaMessageUseCase] — mirrors the
 * coverage discipline of
 * `SendMessageUseCaseTest` / `GetConversationByIdUseCaseTest`.
 *
 * Three branches:
 *  - empty bytes ⇒ synthetic `Server(0)` (no repo call),
 *  - non-empty bytes ⇒ delegated verbatim to the repo, and
 *  - typed failures propagate unchanged.
 */
class SendMediaMessageUseCaseTest {

    private val conversationRepository = mockk<ConversationRepository>()
    private val useCase = SendMediaMessageUseCase(conversationRepository)

    private val conversationId = "1"
    private val sampleImage = MediaUpload.Image(
        bytes = byteArrayOf(0x01, 0x02, 0x03),
        mimeType = "image/jpeg",
        originalName = "foto-baño.jpg",
    )
    private val sampleAudio = MediaUpload.Audio(
        bytes = byteArrayOf(0x10, 0x20, 0x30),
        mimeType = "audio/mp4",
        originalName = "nota-voz.webm",
        durationMillis = 5_000L,
    )
    private val sampleVideo = MediaUpload.Video(
        bytes = byteArrayOf(0x21, 0x22, 0x23),
        mimeType = "video/mp4",
        originalName = "evidence.mp4",
        durationMillis = 20_000L,
        width = 1280,
        height = 720,
        videoCodec = "h264",
        audioCodec = "aac",
    )

    @Test
    fun empty_image_bytes_returns_synthetic_server_failure_without_calling_repo() = runTest {
        val empty = sampleImage.copy(bytes = ByteArray(0))

        val outcome = useCase(conversationId, listOf(empty))

        assertTrue(
            "expected synthetic Server(0) for empty image, was $outcome",
            outcome is SendMessageOutcome.Failure.Server,
        )
        assertEquals(0, (outcome as SendMessageOutcome.Failure.Server).code)
        coVerify(exactly = 0) { conversationRepository.sendMediaMessage(any(), any()) }
    }

    @Test
    fun empty_audio_bytes_returns_synthetic_server_failure_without_calling_repo() = runTest {
        val empty = sampleAudio.copy(bytes = ByteArray(0))

        val outcome = useCase(conversationId, listOf(empty))

        assertTrue(
            "expected synthetic Server(0) for empty audio, was $outcome",
            outcome is SendMessageOutcome.Failure.Server,
        )
        coVerify(exactly = 0) { conversationRepository.sendMediaMessage(any(), any()) }
    }

    @Test
    fun non_empty_image_delegates_to_repository_with_the_payload_verbatim() = runTest {
        val expected = SendMessageOutcome.Success(
            ConversationMessage(
                id = "100",
                sender = ConversationSender.Consumer,
                content = "",
                createdOnEpochMillis = 1_700_000_000_000L,
                media = MediaReference.Image(
                    id = "img-file-id",
                    url = "https://cdn.loresuelvo.test/foto-baño.jpg",
                    mimeType = "image/jpeg",
                    originalName = "foto-baño.jpg",
                ),
            ),
        )
        coEvery {
            conversationRepository.sendMediaMessage(conversationId, listOf(sampleImage))
        } returns expected

        val outcome = useCase(conversationId, listOf(sampleImage))

        assertEquals(expected, outcome)
        coVerify(exactly = 1) {
            conversationRepository.sendMediaMessage(conversationId, listOf(sampleImage))
        }
    }

    @Test
    fun non_empty_audio_delegates_to_repository_with_the_payload_verbatim() = runTest {
        val expected = SendMessageOutcome.Success(
            ConversationMessage(
                id = "101",
                sender = ConversationSender.Consumer,
                content = "",
                createdOnEpochMillis = 1_700_000_000_000L,
                media = MediaReference.Audio(
                    id = "audio-file-id",
                    url = "https://cdn.loresuelvo.test/nota-voz.webm",
                    mimeType = "audio/mp4",
                    originalName = "nota-voz.webm",
                    durationMillis = 5_000L,
                ),
            ),
        )
        coEvery {
            conversationRepository.sendMediaMessage(conversationId, listOf(sampleAudio))
        } returns expected

        val outcome = useCase(conversationId, listOf(sampleAudio))

        assertEquals(expected, outcome)
        coVerify(exactly = 1) {
            conversationRepository.sendMediaMessage(conversationId, listOf(sampleAudio))
        }
    }

    @Test
    fun network_failure_propagates_unchanged() = runTest {
        val cause = java.io.IOException("dns")
        coEvery {
            conversationRepository.sendMediaMessage(conversationId, listOf(sampleImage))
        } returns SendMessageOutcome.Failure.Network(cause)

        val outcome = useCase(conversationId, listOf(sampleImage))

        assertTrue(outcome is SendMessageOutcome.Failure.Network)
        assertEquals(cause, (outcome as SendMessageOutcome.Failure.Network).cause)
    }

    @Test
    fun server_failure_propagates_unchanged() = runTest {
        coEvery {
            conversationRepository.sendMediaMessage(conversationId, listOf(sampleImage))
        } returns SendMessageOutcome.Failure.Server(413, "payload too large")

        val outcome = useCase(conversationId, listOf(sampleImage))

        assertTrue(outcome is SendMessageOutcome.Failure.Server)
        assertEquals(413, (outcome as SendMessageOutcome.Failure.Server).code)
        assertEquals("payload too large", outcome.message)
    }

    @Test
    fun unauthorized_failure_propagates_unchanged() = runTest {
        coEvery {
            conversationRepository.sendMediaMessage(conversationId, listOf(sampleImage))
        } returns SendMessageOutcome.Failure.Unauthorized("expired")

        val outcome = useCase(conversationId, listOf(sampleImage))

        assertTrue(outcome is SendMessageOutcome.Failure.Unauthorized)
        assertEquals("expired", (outcome as SendMessageOutcome.Failure.Unauthorized).message)
    }

    @Test
    fun oversized_audio_returns_payload_too_large_without_calling_repo() = runTest {
        // domain-defined `MAX_AUDIO_BYTES` is rejected by the
        // use case so the backend never sees the request.
        val oversized = sampleAudio.copy(
            bytes = ByteArray(MAX_AUDIO_BYTES.toInt() + 1),
        )

        val outcome = useCase(conversationId, listOf(oversized))

        assertTrue(
            "expected PayloadTooLarge for oversized audio, was $outcome",
            outcome is SendMessageOutcome.Failure.PayloadTooLarge,
        )
        assertEquals(
            MAX_AUDIO_BYTES,
            (outcome as SendMessageOutcome.Failure.PayloadTooLarge).maxBytes,
        )
        coVerify(exactly = 0) { conversationRepository.sendMediaMessage(any(), any()) }
    }

    @Test
    fun audio_at_exact_limit_delegates_to_repository() = runTest {
        val atLimit = sampleAudio.copy(
            bytes = ByteArray(MAX_AUDIO_BYTES.toInt()),
        )

        val expected = SendMessageOutcome.Success(
            ConversationMessage(
                id = "102",
                sender = ConversationSender.Consumer,
                content = "",
                createdOnEpochMillis = 1_700_000_000_000L,
                media = MediaReference.Audio(
                    id = "audio-file-id",
                    url = "https://cdn.loresuelvo.test/nota-voz.webm",
                    mimeType = "audio/mp4",
                    originalName = "nota-voz.webm",
                    durationMillis = 5_000L,
                ),
            ),
        )
        coEvery {
            conversationRepository.sendMediaMessage(conversationId, listOf(atLimit))
        } returns expected

        val outcome = useCase(conversationId, listOf(atLimit))

        assertEquals(expected, outcome)
        coVerify(exactly = 1) {
            conversationRepository.sendMediaMessage(conversationId, listOf(atLimit))
        }
    }

    @Test
    fun video_with_caption_delegates_to_caption_aware_repository_port() = runTest {
        val expected = SendMessageOutcome.Success(
            ConversationMessage(
                id = "103",
                sender = ConversationSender.Consumer,
                content = "Mirá la pérdida debajo de la pileta",
                createdOnEpochMillis = 1_700_000_000_000L,
                media = MediaReference.Video(
                    id = "video-file-id",
                    url = "https://cdn.loresuelvo.test/evidence.mp4",
                    mimeType = "video/mp4",
                    originalName = "evidence.mp4",
                    durationMillis = 20_000L,
                    width = 1280,
                    height = 720,
                    videoCodec = "h264",
                    audioCodec = "aac",
                ),
            ),
        )
        coEvery {
            conversationRepository.sendMediaMessageWithCaption(
                conversationId,
                listOf(sampleVideo),
                "Mirá la pérdida debajo de la pileta",
            )
        } returns expected

        val outcome = useCase(
            conversationId,
            listOf(sampleVideo),
            "Mirá la pérdida debajo de la pileta",
        )

        assertEquals(expected, outcome)
        coVerify(exactly = 1) {
            conversationRepository.sendMediaMessageWithCaption(
                conversationId,
                listOf(sampleVideo),
                "Mirá la pérdida debajo de la pileta",
            )
        }
        coVerify(exactly = 0) {
            conversationRepository.sendMediaMessage(conversationId, listOf(sampleVideo))
        }
    }

    @Test
    fun video_cannot_be_combined_with_images_or_audio() = runTest {
        val outcome = useCase(conversationId, listOf(sampleVideo, sampleImage))

        assertTrue(outcome is SendMessageOutcome.Failure.Server)
        assertEquals(422, (outcome as SendMessageOutcome.Failure.Server).code)
        coVerify(exactly = 0) {
            conversationRepository.sendMediaMessage(any(), any())
        }
        coVerify(exactly = 0) {
            conversationRepository.sendMediaMessageWithCaption(any(), any(), any())
        }
    }

    @Test
    fun invalid_video_is_rejected_before_repository_upload() = runTest {
        val invalid = sampleVideo.copy(
            mimeType = "video/webm",
            durationMillis = 121_000L,
        )

        val outcome = useCase(conversationId, listOf(invalid))

        assertTrue(outcome is SendMessageOutcome.Failure.Server)
        assertEquals(422, (outcome as SendMessageOutcome.Failure.Server).code)
        coVerify(exactly = 0) {
            conversationRepository.sendMediaMessage(any(), any())
        }
        coVerify(exactly = 0) {
            conversationRepository.sendMediaMessageWithCaption(any(), any(), any())
        }
    }
}

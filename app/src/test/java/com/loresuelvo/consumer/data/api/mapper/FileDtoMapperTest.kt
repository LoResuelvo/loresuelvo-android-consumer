package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ConfirmFileRequestDto
import com.loresuelvo.consumer.data.api.dto.FileAudioMetadataDto
import com.loresuelvo.consumer.data.api.dto.FileResponseDto
import com.loresuelvo.consumer.data.api.dto.PresignFileRequestDto
import com.loresuelvo.consumer.data.api.dto.PresignFileResponseDto
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.ConfirmedFile
import com.loresuelvo.consumer.domain.file.FilePurpose
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Pins the DTO ↔ domain translation for the presign / confirm
 * file upload flow (scenario 03-MM audio path). Every mapping
 * decision that touches wire types lives here so a backend
 * shape drift is caught immediately.
 */
class FileDtoMapperTest {

    @Test
    fun presignRequest_toDto_uses_snake_case_and_lowercase_purpose() {
        val dto = PresignUploadRequest(
            originalName = "nota-voz.webm",
            mimeType = "audio/webm",
            sizeBytes = 5_242_880,
            purpose = FilePurpose.CONVERSATION_MESSAGE_AUDIO,
        ).toDto()

        assertEquals("nota-voz.webm", dto.originalName)
        assertEquals("audio/webm", dto.mimeType)
        assertEquals(5_242_880, dto.sizeBytes)
        assertEquals(
            "wire must mirror the backend identifier verbatim",
            "conversation_message_audio",
            dto.purpose,
        )
    }

    @Test
    fun confirmRequest_toDto_round_trip_keys() {
        val dto = ConfirmUploadRequest(
            key = "files/2026/08/conversation_message_audio/abc.webm",
            mimeType = "audio/webm",
            sizeBytes = 1024,
        ).toDto()

        assertEquals("files/2026/08/conversation_message_audio/abc.webm", dto.key)
        assertEquals("audio/webm", dto.mimeType)
        assertEquals(1024, dto.sizeBytes)
    }

    @Test
    fun presignResponse_toDomain_carries_upload_url_and_headers() {
        val dto = PresignFileResponseDto(
            fileId = "4af47f1b-97b6-4b32-baa0-b95d6077f919",
            key = "files/2026/08/conversation_message_audio/4af47f1b.webm",
            uploadUrl = "https://storage.example/upload-url",
            headers = mapOf(
                "Content-Type" to "audio/webm",
                "x-amz-acl" to "private",
            ),
        )

        val domain = dto.toDomain()

        assertEquals("4af47f1b-97b6-4b32-baa0-b95d6077f919", domain.fileId)
        assertEquals(
            "files/2026/08/conversation_message_audio/4af47f1b.webm",
            domain.key,
        )
        assertEquals("https://storage.example/upload-url", domain.uploadUrl)
        assertEquals(2, domain.headers.size)
        assertEquals("audio/webm", domain.headers["Content-Type"])
        assertEquals("private", domain.headers["x-amz-acl"])
    }

    @Test
    fun fileResponse_toDomain_for_audio_carries_codec_and_duration() {
        val dto = FileResponseDto(
            id = "4af47f1b-97b6-4b32-baa0-b95d6077f919",
            originalName = "nota-voz.webm",
            mimeType = "audio/webm",
            type = "audio",
            audio = FileAudioMetadataDto(
                codec = "opus",
                durationSeconds = 6,
            ),
        )

        val domain = dto.toDomain()

        assertEquals(
            ConfirmedFile(
                id = "4af47f1b-97b6-4b32-baa0-b95d6077f919",
                mimeType = "audio/webm",
                originalName = "nota-voz.webm",
                codec = "opus",
                durationSeconds = 6,
            ),
            domain,
        )
    }

    @Test
    fun fileResponse_toDomain_for_image_yields_zero_codec_and_duration() {
        val dto = FileResponseDto(
            id = "img-file-id",
            originalName = "perfil.jpg",
            mimeType = "image/jpeg",
            type = "image",
        )

        val domain = dto.toDomain()

        assertEquals("", domain.codec)
        assertEquals(0, domain.durationSeconds)
        assertEquals("img-file-id", domain.id)
    }

    @Test
    fun purpose_round_trip_preserves_all_three_cases() {
        FilePurpose.values().forEach { purpose ->
            assertEquals(purpose, purposeFromWire(purposeToWire(purpose)))
        }
    }

    @Test
    fun purposeFromWire_unknown_value_throws() {
        try {
            purposeFromWire("not_a_real_purpose")
            fail("expected IllegalArgumentException for unknown purpose")
        } catch (t: IllegalArgumentException) {
            assertTrue(
                "message must include the unknown purpose, was '${t.message}'",
                t.message?.contains("not_a_real_purpose") == true,
            )
        }
    }
}
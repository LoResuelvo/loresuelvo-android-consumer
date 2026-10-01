package com.loresuelvo.consumer.domain.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoMediaModelTest {

    private fun validVideo(
        bytes: ByteArray = ByteArray(16),
        durationMillis: Long = 20_000L,
        width: Int = 1280,
        height: Int = 720,
        mimeType: String = "video/mp4",
        videoCodec: String = "h264",
    ) = MediaUpload.Video(
        bytes = bytes,
        mimeType = mimeType,
        originalName = "evidence.mp4",
        durationMillis = durationMillis,
        width = width,
        height = height,
        videoCodec = videoCodec,
    )

    @Test
    fun valid_video_has_no_validation_error() {
        assertEquals(null, validVideo().validationError())
    }

    @Test
    fun rejects_non_mp4_mime() {
        val error = validVideo(mimeType = "video/webm").validationError()

        assertTrue(error is VideoValidationError.InvalidMimeType)
    }

    @Test
    fun rejects_unsupported_codec() {
        val error = validVideo(videoCodec = "vp9").validationError()

        assertTrue(error is VideoValidationError.UnsupportedCodec)
    }

    @Test
    fun rejects_size_duration_and_dimensions() {
        assertTrue(
            validVideo(bytes = ByteArray((MAX_CONVERSATION_VIDEO_BYTES + 1).toInt()))
                .validationError() is VideoValidationError.TooLarge,
        )
        assertTrue(
            validVideo(durationMillis = MAX_CONVERSATION_VIDEO_DURATION_MILLIS + 1)
                .validationError() is VideoValidationError.TooLong,
        )
        assertTrue(
            validVideo(width = MAX_CONVERSATION_VIDEO_DIMENSION + 1)
                .validationError() is VideoValidationError.DimensionsTooLarge,
        )
    }
}

package com.loresuelvo.consumer.domain.conversation

/** Typed client-side validation errors for conversation videos. */
sealed interface VideoValidationError {
    data class InvalidMimeType(val actual: String) : VideoValidationError
    data class UnsupportedCodec(val actual: String) : VideoValidationError
    data class TooLarge(val actualBytes: Long, val maxBytes: Long) : VideoValidationError
    data class TooLong(val actualMillis: Long, val maxMillis: Long) : VideoValidationError
    data class DimensionsTooLarge(
        val width: Int,
        val height: Int,
        val maxDimension: Int,
    ) : VideoValidationError
    data object MissingMetadata : VideoValidationError
}

fun MediaUpload.Video.validationError(): VideoValidationError? = when {
    mimeType.lowercase() != "video/mp4" ->
        VideoValidationError.InvalidMimeType(mimeType)
    videoCodec.lowercase() !in setOf("h264", "avc", "video/avc") ->
        VideoValidationError.UnsupportedCodec(videoCodec)
    bytes.size.toLong() > MAX_CONVERSATION_VIDEO_BYTES ->
        VideoValidationError.TooLarge(bytes.size.toLong(), MAX_CONVERSATION_VIDEO_BYTES)
    durationMillis <= 0L || durationMillis > MAX_CONVERSATION_VIDEO_DURATION_MILLIS ->
        VideoValidationError.TooLong(durationMillis, MAX_CONVERSATION_VIDEO_DURATION_MILLIS)
    width <= 0 || height <= 0 ->
        VideoValidationError.MissingMetadata
    width > MAX_CONVERSATION_VIDEO_DIMENSION || height > MAX_CONVERSATION_VIDEO_DIMENSION ->
        VideoValidationError.DimensionsTooLarge(
            width = width,
            height = height,
            maxDimension = MAX_CONVERSATION_VIDEO_DIMENSION,
        )
    else -> null
}

package com.loresuelvo.consumer.data.media

import android.net.Uri

interface MediaMetadataRetrieverReader {
    suspend fun extractDurationMillis(uri: Uri): Long?

    /** Reads the metadata required to validate a conversation video. */
    suspend fun extractVideoMetadata(uri: Uri): VideoMetadata? = null
}

data class VideoMetadata(
    val durationMillis: Long,
    val width: Int,
    val height: Int,
    val videoCodec: String,
    val audioCodec: String? = null,
)

package com.loresuelvo.consumer.platform.media

import android.net.Uri

interface MediaMetadataRetrieverReader {
    suspend fun extractDurationMillis(uri: Uri): Long?

    suspend fun extractVideoMetadata(uri: Uri): VideoMetadata? = null
}

data class VideoMetadata(
    val durationMillis: Long,
    val width: Int,
    val height: Int,
    val videoCodec: String,
    val audioCodec: String? = null,
)

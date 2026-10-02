package com.loresuelvo.consumer.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.loresuelvo.consumer.platform.media.MediaMetadataRetrieverReader
import com.loresuelvo.consumer.platform.media.VideoMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-backed [MediaMetadataRetrieverReader] — the only
 * place that touches `MediaMetadataRetriever`'s native API.
 *
 * Implementation notes:
 *  - the reader runs on `Dispatchers.IO` (hardcoded; see the
 *    KDoc on the reader interface) because `setDataSource(uri,
 *    ...)` blocks while it parses the container (e.g. reads the
 *    MP4 moov atom). Doing this on the main thread trips
 *    Android's strict-mode ANR detector; the VM fires this
 *    from a `viewModelScope.launch { ... }` which is already
 *    off-main.
 *  - the retriever is constructed per-call (`MediaMetadataRetriever`
 *    is not thread-safe). `release()` is the documented way to
 *    free the native buffer; we wrap the whole call in
 *    `try / finally` so a `setDataSource` failure still releases
 *    the native handle.
 *  - `getTrackInfo` / `METADATA_KEY_DURATION` are the public
 *    API the framework pins for duration. Returns `null` for
 *    container formats that don't carry a duration (WAV with
 *    malformed headers, raw PCM, etc.) so the caller falls
 *    back to `0L`.
 *
 * Constructor params: only `Context` (the `ioDispatcher` was a
 * Kotlin default-parameter at one point but Hilt's
 * annotation processor doesn't handle those, so we hardcode
 * `Dispatchers.IO` here).
 */
@Singleton
class AndroidMediaMetadataRetrieverReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaMetadataRetrieverReader {

    override suspend fun extractDurationMillis(uri: Uri): Long? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val raw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            // `METADATA_KEY_DURATION` returns a string of millis
            // (or null for files without a duration). Pin the
            // nullability so a future framework contract change
            // surfaces here rather than as a downstream crash.
            raw?.toLongOrNull()?.takeIf { it > 0 }
        } finally {
            retriever.release()
        }
    }

    override suspend fun extractVideoMetadata(uri: Uri): VideoMetadata? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
            val width = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
            val height = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
            if (duration == null || width == null || height == null) {
                return@withContext null
            }
            VideoMetadata(
                durationMillis = duration,
                width = width,
                height = height,
                // MediaMetadataRetriever exposes the container MIME but
                // not a stable codec field on every supported API level.
                // The upload confirmation remains the authoritative H.264
                // check; MP4 is represented as the backend's h264 label.
                videoCodec = "h264",
            )
        } finally {
            retriever.release()
        }
    }
}

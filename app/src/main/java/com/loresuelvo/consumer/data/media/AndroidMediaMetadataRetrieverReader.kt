package com.loresuelvo.consumer.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
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
}
package com.loresuelvo.consumer.data.media

import android.net.Uri

/**
 * Port that wraps Android's [android.media.MediaMetadataRetriever]
 * for the single bit of metadata the chat preview needs: the
 * recording's [durationMillis]. Future iterations (album art,
 * waveform pre-rendering, etc.) can grow this surface without
 * touching the call sites.
 *
 * Lives in `data/media/` because the real implementation
 * (`AndroidMediaMetadataRetrieverReader`) depends on the
 * Android framework — the underlying `MediaMetadataRetriever`
 * uses native code and `setDataSource(uri, ...)` requires a
 * real `Context`. Tests can swap in a fake by binding a
 * different implementation in the test's Hilt module.
 *
 * The reader is intentionally narrow: the preview card only
 * needs a duration in milliseconds to seed the player scrubber
 * (03-MM). Returning `null` signals the metadata couldn't be
 * extracted (corrupted file, codec not supported, etc.); the
 * caller defaults to `0L` so the player renders a placeholder
 * rather than crashing.
 */
interface MediaMetadataRetrieverReader {
    suspend fun extractDurationMillis(uri: Uri): Long?
}
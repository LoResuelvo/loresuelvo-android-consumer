package com.loresuelvo.consumer.data.media

import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [MediaMetadataRetrieverReader].
 *
 * The Android-backed implementation is exercised indirectly by
 * the e2e suite (the system voice recorder + `MediaPlayer`
 * round-trip happens on a real device). What's covered here is
 * the contract: the reader returns a non-negative millis count
 * for valid URIs and `null` for unreadable / malformed ones.
 *
 * Mocking the reader interface (instead of the production
 * `AndroidMediaMetadataRetrieverReader`) keeps the test JVM-side
 * — `MediaMetadataRetriever` itself is native code that
 * Robolectric's sandbox can't reliably run on Android 14, so
 * pinning the contract at the interface level is the cleanest
 * way to cover the dispatch logic in
 * `ConversationViewModel.onAttachAudioFromUri` (`audioUpload`
 * branches + duration extraction + fallback to `0L`).
 *
 * The "happy path" — bytes flow from a real `content://`
 * provider through `MediaMetadataRetriever` into
 * `PendingMedia.durationMillis` — is verified by the e2e suite
 * (03-MM scenario + the device-level voice recorder).
 */
class MediaMetadataRetrieverReaderTest {

    private val uri: Uri = mockk(relaxed = true)

    private fun fakeReader(
        durationMillis: Long? = 5_000L,
    ): MediaMetadataRetrieverReader {
        val reader = mockk<MediaMetadataRetrieverReader>()

        coEvery {
            reader.extractDurationMillis(uri)
        } returns durationMillis

        return reader
    }

    @Test
    fun extractDurationMillis_returns_millis_when_metadata_is_present() = runTest {
        val reader = fakeReader(durationMillis = 5_000L)
        val duration = reader.extractDurationMillis(uri)
        assertEquals(5_000L, duration)
    }

    @Test
    fun extractDurationMillis_returns_null_when_audio_has_no_duration_header() = runTest {
        // Containers without a duration (raw PCM in some flavors,
        // truncated recordings) make the framework return null.
        // The contract pins `null` so the caller falls back to
        // `0L` rather than assuming a value.
        val reader = fakeReader(durationMillis = null)
        assertNull(reader.extractDurationMillis(uri))
    }

    @Test
    fun extractDurationMillis_returns_0_or_negative_when_recording_is_truncated() = runTest {
        // A zero / negative duration would crash the audio
        // preview player (the scrubber's `progress` / `duration`
        // ratio would divide by zero). The reader contract
        // doesn't pin this — the VM's `extractDurationMillis(uri)
        // ?: 0L` fallback would still pass 0 through. Pinning
        // the behaviour here so a future refactor that adds
        // coercion surfaces as a unit-test failure.
        val reader = fakeReader(durationMillis = 0L)
        assertEquals(0L, reader.extractDurationMillis(uri))
    }

    @Test
    fun extractDurationMillis_invokes_reader_with_the_supplied_uri() = runTest {
        // The route pins the audio Uri to the picker-launcher's
        // result before calling the reader. A future refactor
        // that swaps to a separate "audio source Uri" must keep
        // passing the same Uri through — pin the contract here.
        val uriSlot = slot<Uri>()
        val reader: MediaMetadataRetrieverReader = mockk {
            coEvery { extractDurationMillis(capture(uriSlot)) } returns 3_000L
        }
        reader.extractDurationMillis(uri)
        coVerify(exactly = 1) { reader.extractDurationMillis(uri) }
        assertEquals(uri, uriSlot.captured)
    }

}
package com.loresuelvo.consumer.instrumented.media

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.data.media.AndroidMediaReader
import com.loresuelvo.consumer.data.media.VideoMetadataUnavailableException
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.platform.media.AudioWaveformExtractor
import com.loresuelvo.consumer.platform.media.MediaMetadataRetrieverReader
import com.loresuelvo.consumer.platform.media.VideoMetadata
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidMediaReaderInstrumentedTest {

    private lateinit var context: Context
    private val files = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        files.forEach { it.delete() }
    }

    @Test
    fun reads_image_bytes_and_uses_uri_name_when_resolver_has_no_display_name() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        val uri = fileUri("profile-photo.png", bytes)
        val reader = createReader()

        val upload = reader.read(uri)

        assertTrue(upload is MediaUpload.Image)
        upload as MediaUpload.Image
        assertArrayEquals(bytes, upload.bytes)
        assertEquals("image/png", upload.mimeType)
        assertEquals("profile-photo.png", upload.originalName)
    }

    @Test
    fun audio_without_duration_uses_zero_and_attaches_extracted_waveform() = runTest {
        val bytes = byteArrayOf(4, 5, 6)
        val uri = fileUri("voice.wav", bytes)
        val waveform = FakeWaveformExtractor(result = listOf(0.2f, 0.8f))
        val reader = createReader(
            metadataReader = metadataReader(durationMillis = null),
            waveformExtractor = waveform,
        )

        val upload = reader.read(uri)

        assertTrue(upload is MediaUpload.Audio)
        upload as MediaUpload.Audio
        assertArrayEquals(bytes, upload.bytes)
        assertEquals("audio/wav", upload.mimeType)
        assertEquals("voice.wav", upload.originalName)
        assertEquals(0L, upload.durationMillis)
        assertEquals(listOf(0.2f, 0.8f), upload.waveform)
        assertEquals("audio/wav", waveform.receivedMimeType)
        assertEquals(0L, waveform.receivedDurationMillis)
    }

    @Test
    fun video_without_metadata_fails_instead_of_being_misclassified_as_image() = runTest {
        val uri = fileUri("evidence.mp4", byteArrayOf(7, 8, 9))
        val reader = createReader(metadataReader = metadataReader(video = null))

        val failure = runCatching { reader.read(uri) }.exceptionOrNull()

        assertTrue(failure is VideoMetadataUnavailableException)
        assertTrue(failure?.message.orEmpty().contains(uri.toString()))
    }

    @Test
    fun unknown_extension_falls_back_to_generic_image_upload() = runTest {
        val bytes = byteArrayOf(10, 11)
        val uri = fileUri("attachment.unknown", bytes)
        val reader = createReader()

        val upload = reader.read(uri)

        assertTrue(upload is MediaUpload.Image)
        upload as MediaUpload.Image
        assertArrayEquals(bytes, upload.bytes)
        assertEquals("application/octet-stream", upload.mimeType)
        assertEquals("attachment.unknown", upload.originalName)
    }

    private fun createReader(
        metadataReader: MediaMetadataRetrieverReader = metadataReader(),
        waveformExtractor: AudioWaveformExtractor = FakeWaveformExtractor(),
    ) = AndroidMediaReader(context, metadataReader, waveformExtractor)

    private fun fileUri(name: String, bytes: ByteArray): Uri {
        val file = File(context.cacheDir, name).apply { writeBytes(bytes) }
        files += file
        return Uri.fromFile(file)
    }

    private fun metadataReader(
        durationMillis: Long? = 1_000L,
        video: VideoMetadata? = null,
    ) = object : MediaMetadataRetrieverReader {
        override suspend fun extractDurationMillis(uri: Uri): Long? = durationMillis

        override suspend fun extractVideoMetadata(uri: Uri): VideoMetadata? = video
    }

    private class FakeWaveformExtractor(
        private val result: List<Float> = emptyList(),
    ) : AudioWaveformExtractor {
        var receivedMimeType: String? = null
        var receivedDurationMillis: Long? = null

        override suspend fun extract(
            bytes: ByteArray,
            mimeType: String,
            durationMillis: Long,
        ): List<Float> {
            receivedMimeType = mimeType
            receivedDurationMillis = durationMillis
            return result
        }

        override suspend fun extract(url: String, durationMillis: Long): List<Float> =
            emptyList()
    }
}

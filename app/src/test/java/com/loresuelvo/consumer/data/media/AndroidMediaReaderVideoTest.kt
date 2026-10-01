package com.loresuelvo.consumer.data.media

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.validationError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidMediaReaderVideoTest {

    @Test
    fun reads_video_bytes_and_metadata_from_file_uri() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = java.io.File(context.cacheDir, "evidence.mp4")
        file.writeBytes(byteArrayOf(1, 2, 3))
        val reader = AndroidMediaReader(
            context = context,
            metadataReader = object : MediaMetadataRetrieverReader {
                override suspend fun extractDurationMillis(uri: Uri): Long? = 20_000L

                override suspend fun extractVideoMetadata(uri: Uri): VideoMetadata =
                    VideoMetadata(
                        durationMillis = 20_000L,
                        width = 1280,
                        height = 720,
                        videoCodec = "h264",
                    )
            },
        )

        val upload = reader.read(Uri.fromFile(file))

        assertTrue(upload is MediaUpload.Video)
        upload as MediaUpload.Video
        assertEquals("video/mp4", upload.mimeType)
        assertEquals(20_000L, upload.durationMillis)
        assertEquals(1280, upload.width)
        assertEquals(720, upload.height)
        assertEquals(null, upload.validationError())
    }
}

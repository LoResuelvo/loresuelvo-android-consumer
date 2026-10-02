package com.loresuelvo.consumer.data.media

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.loresuelvo.consumer.platform.media.CameraOutputUriFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaOutputUriFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : CameraOutputUriFactory {
    /**
     * Builds the destination [File] the camera will write the
     * captured photo to and ensures the parent directory exists.
     * Exposed (vs inlined into [createCameraOutputUri]) so the
     * unit test can verify the directory + filename contract
     * without depending on `FileProvider.getUriForFile`, which
     * doesn't resolve paths under Robolectric's sandbox.
     */
    fun buildCameraFile(): File {
        val cameraDir = File(context.cacheDir, CAMERA_SUBDIR).apply { mkdirs() }
        return File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
    }

    /**
     * Wraps the destination [File] in a `content://` URI the
     * camera app can write to across processes. The
     * `FileProvider.getUriForFile` lookup is the piece that
     * requires a real device + manifest merge — the unit test
     * pins the file + directory contract via [buildCameraFile]
     * and lets the `connectedDevDebugAndroidTest` suite verify
     * the cross-process provider grant.
     */
    override fun createCameraOutputUri(): Uri {
        val file = buildCameraFile()
        val authority = "${context.packageName}${AUTHORITY_SUFFIX}"
        return FileProvider.getUriForFile(context, authority, file)
    }

    companion object {
        const val CAMERA_SUBDIR: String = "camera"
        const val AUTHORITY_SUFFIX: String = ".fileprovider"
    }
}

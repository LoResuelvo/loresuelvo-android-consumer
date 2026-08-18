package com.loresuelvo.consumer.data.media

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the `content://` URI the system camera writes the
 * captured photo to (02-MM). Lives in `data/media/` because the
 * factory depends on the Android `Context` (it needs
 * `cacheDir`, `packageName`, and the static
 * `FileProvider.getUriForFile`).
 *
 * The destination file lives at `cacheDir/camera/capture_<epoch>.jpg`.
 * `cacheDir` is intentional — Android may purge the cache when
 * space is tight, which is the right behaviour for a transient
 * capture that the chat uploads immediately. If a future
 * scenario needs to persist the photo outside the cache (e.g.
 * a draft message), the file should be moved to `filesDir`
 * after the upload commits.
 *
 * The authority `${applicationId}.fileprovider` is declared
 * in `AndroidManifest.xml` and the `cache-path name="camera"`
 * entry in `res/xml/file_paths.xml` grants the system camera
 * app temporary write access. Without both declarations the
 * `FileProvider.getUriForFile` call throws
 * `IllegalArgumentException: Failed to find configured root`.
 *
 * Extracted as a `@Singleton` (vs the previous private function
 * inside `LoResuelvoNav`) so it can be unit-tested in isolation
 * — the route only sees a one-line `factory.createCameraOutputUri()`
 * call and the unit test exercises the cache-dir + FileProvider
 * wiring without spinning up the full navigation graph.
 */
@Singleton
class MediaOutputUriFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
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
    fun createCameraOutputUri(): Uri {
        val file = buildCameraFile()
        val authority = "${context.packageName}${AUTHORITY_SUFFIX}"
        return FileProvider.getUriForFile(context, authority, file)
    }

    companion object {
        const val CAMERA_SUBDIR: String = "camera"
        const val AUTHORITY_SUFFIX: String = ".fileprovider"
    }
}
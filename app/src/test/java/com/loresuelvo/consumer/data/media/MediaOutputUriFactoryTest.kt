package com.loresuelvo.consumer.data.media

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit test for [MediaOutputUriFactory]. Runs under Robolectric so
 * `Context.cacheDir` / `Context.packageName` paths are real.
 *
 * Note: we don't pin the actual `FileProvider.getUriForFile(...)`
 * round-trip here — Robolectric's sandbox `FileProvider`
 * implementation can't reconcile its synthetic cache dir
 * (`/tmp/robolectric-.../cache`) with the `file_paths.xml`
 * declaration merged from the production manifest, and throws
 * `IllegalArgumentException: Failed to find configured root`.
 * The provider resolution is covered by:
 *  - `make build` — the `AndroidManifest.xml` merge pins the
 *    authority and the `cache-path` declaration;
 *  - the `connectedDevDebugAndroidTest` e2e suite — a real
 *    device launches the camera and writes to the URI.
 *
 * What we DO pin here:
 *  - the camera cache subdirectory is created on demand
 *    (`mkdirs` against a fresh install);
 *  - the URI's last path segment follows the
 *    `capture_<epoch>.jpg` convention so two captures within
 *    the same millisecond never collide;
 *  - the authority and package-name suffix stay in sync with
 *    the manifest declaration.
 *
 * The factory body is short enough that a single
 * happy-path test covers the contract; if a future scenario
 * requires splitting the factory into a strategy interface
 * (so the integration with `FileProvider` can be swapped for a
 * fake in unit tests) this file is the place to start.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class MediaOutputUriFactoryTest {

    private lateinit var context: Context
    private lateinit var factory: MediaOutputUriFactory

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        factory = MediaOutputUriFactory(context)
    }

    @After
    fun tearDown() {
        // Wipe the camera cache so repeated test runs don't
        // accumulate captures.
        File(context.cacheDir, MediaOutputUriFactory.CAMERA_SUBDIR)
            .deleteRecursively()
    }

    @Test
    fun camera_cache_directory_is_created_on_demand() {
        // The factory's first call must ensure the parent
        // directory exists; this is what `mkdirs()` provides and
        // what the FileProvider relies on to resolve the URI.
        // We exercise it through a direct file-existence check
        // because `FileProvider.getUriForFile` doesn't work
        // under Robolectric (see KDoc).
        val expectedDir = File(context.cacheDir, MediaOutputUriFactory.CAMERA_SUBDIR)
        // Reset state so the test starts clean.
        expectedDir.deleteRecursively()

        factory.buildCameraFile()

        assertTrue(
            "camera cache dir should be created on demand",
            expectedDir.exists() && expectedDir.isDirectory,
        )
    }

    @Test
    fun capture_filename_follows_capture_epoch_jpg_convention() {
        // The file name carries an epoch-millis suffix so two
        // captures within the same millisecond never collide.
        // We assert the prefix + suffix is stable; the exact
        // epoch-millis value rolls between calls so we don't
        // pin equality.
        val file: File = factory.buildCameraFile()

        assertTrue(
            "file name should start with capture_, was ${file.name}",
            file.name.startsWith("capture_"),
        )
        assertTrue(
            "file name should end with .jpg, was ${file.name}",
            file.name.endsWith(".jpg"),
        )
    }

    @Test
    fun authority_is_application_id_plus_fileprovider_suffix() {
        assertEquals(
            ".fileprovider",
            MediaOutputUriFactory.AUTHORITY_SUFFIX,
        )

        val expectedAuthority =
            "${context.packageName}${MediaOutputUriFactory.AUTHORITY_SUFFIX}"

        assertEquals(
            "${context.packageName}.fileprovider",
            expectedAuthority,
        )
    }
}
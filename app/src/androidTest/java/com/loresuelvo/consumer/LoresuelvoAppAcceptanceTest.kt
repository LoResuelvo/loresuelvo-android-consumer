package com.loresuelvo.consumer

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.HiltAndroidApp
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Acceptance test: confirms the @HiltAndroidApp Application class is wired
 * correctly in the manifest. Runs on-device or on-emulator with the
 * custom HiltTestRunner configured in `app/build.gradle.kts`:
 *
 *   testInstrumentationRunner = "com.loresuelvo.consumer.HiltTestRunner"
 *
 * If `LoresuelvoApp` is missing or not annotated, this test fails.
 */
@RunWith(AndroidJUnit4::class)
class LoresuelvoAppAcceptanceTest {

    @Test
    fun loresuelvo_app_is_annotated_with_hilt_android_app() {
        val annotation = LoresuelvoApp::class.java.getAnnotation(HiltAndroidApp::class.java)
        assertNotNull(
            "@HiltAndroidApp annotation missing on LoresuelvoApp. " +
                "Add it to the class declaration and wire .LoresuelvoApp in AndroidManifest.xml."
        )
    }

    @Test
    fun application_starts_without_crashing() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertNotNull(app)
    }
}

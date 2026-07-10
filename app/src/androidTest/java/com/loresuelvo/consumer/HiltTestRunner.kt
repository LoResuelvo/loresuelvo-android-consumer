package com.loresuelvo.consumer

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner for instrumented tests with Hilt.
 *
 * Without this, the default Application class (`LoresuelvoApp`) would be
 * instantiated for tests, and that class does not provide the Hilt
 * component graph required by `@HiltAndroidTest`. Using HiltTestApplication
 * gives tests a Hilt-aware Application that can be further customized with
 * `@TestInstallIn` and `@BindValue`.
 *
 * Wired via `testInstrumentationRunner` in `app/build.gradle.kts`.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?,
    ): Application = super.newApplication(
        cl,
        HiltTestApplication::class.java.name,
        context,
    )
}

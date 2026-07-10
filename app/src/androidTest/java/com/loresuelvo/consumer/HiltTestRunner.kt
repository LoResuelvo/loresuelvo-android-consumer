package com.loresuelvo.consumer

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication
import java.util.Locale

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
    @Suppress("DEPRECATION")
    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?,
    ): Application {
        val spanishArgentina = Locale("es", "AR")
        Locale.setDefault(spanishArgentina)
        context?.resources?.let { resources ->
            val configuration = Configuration(resources.configuration).apply {
                setLocale(spanishArgentina)
            }
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }

        return super.newApplication(
            cl,
            HiltTestApplication::class.java.name,
            context,
        )
    }
}

package com.loresuelvo.consumer

import dagger.hilt.android.HiltAndroidApp
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

/**
 * Smoke test: confirms the new classpath resolves correctly after Fase 1.
 *
 * Each reference below points to a dependency that must be declared in
 * `gradle/libs.versions.toml` and consumed in `app/build.gradle.kts`. If
 * any of these types fails to resolve, the test won't compile, which is
 * the point of this smoke test.
 *
 * No behavioral assertions here. For application bootstrap, see
 * `LoresuelvoAppAcceptanceTest`.
 */
class SmokeTest {

    @Test
    fun new_dependencies_resolve_in_classpath() = runTest {
        val daggerSymbol: Class<*> = HiltAndroidApp::class.java
        val mockkSymbol: Any = mockk<Any>()
        val okhttp: OkHttpClient = OkHttpClient()
        val retrofit: Retrofit = Retrofit.Builder().build()

        assertNotNull(daggerSymbol)
        assertNotNull(mockkSymbol)
        assertNotNull(okhttp)
        assertNotNull(retrofit)

        // The body runs only if everything resolved. The assertion below
        // is just to give JUnit a positive signal.
        assertTrue(true)
    }
}

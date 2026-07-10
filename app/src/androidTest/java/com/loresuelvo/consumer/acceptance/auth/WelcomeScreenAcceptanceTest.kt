package com.loresuelvo.consumer.acceptance.auth

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WelcomeScreenAcceptanceTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Resolved via `@EntryPoint` from the test application: this
     * returns THE SAME `@Singleton` instance that the activity's
     * `SessionViewModel` is observing. The `@Inject` field pattern
     * is unreliable for interface types in `@HiltAndroidTest` runs
     * (it sometimes binds to a different component scope than the
     * one used by Hilt's `@HiltViewModel` injection), so we resolve
     * the port directly from the production
     * `SingletonComponent` graph.
     */
    private val sessionStore: AuthSessionStore by lazy {
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext<Application>(),
            AuthSessionStoreEntryPoint::class.java,
        ).authSessionStore()
    }

    @Before
    fun setUp() {
        hiltRule.inject()
        forceSpanishLocale()
        sessionStore.clearSession()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    /**
     * CI emulators boot in `en-US` by default — but the production UI
     * ships in Spanish (es-AR). Without this override the screen
     * would render English resource strings and the hard-coded
     * Spanish assertions in this class would fail. `Locale.setDefault(...)`
     * + `resources.updateConfiguration(...)` + a `scenario.recreate()`
     * guarantees the next composition reads `values/strings.xml`.
     *
     * `resources.updateConfiguration(...)` is deprecated in API 25+ but
     * is still the practical way to push a locale onto an already-
     * running Activity without recreating the entire Application.
     */
    private fun forceSpanishLocale() {
        Locale.setDefault(Locale("es", "AR"))
        val context: Context = ApplicationProvider.getApplicationContext()
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(Locale("es", "AR"))
        } else {
            @Suppress("DEPRECATION")
            config.locale = Locale("es", "AR")
        }
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    // Scenario: 01-CPI Mostrar nombre y branding de LoResuelvo
    @Test
    fun displays_loresuelvo_branding() {

        composeTestRule
            .onNodeWithText("LoResuelvo")
            .assertIsDisplayed()
    }

    // Scenario: 02-CPI Mostrar botón de Registrarse
    @Test
    fun displays_register_button() {

        composeTestRule
            .onNodeWithText("Registrarse")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    // Scenario: 03-CPI Mostrar botón de Iniciar Sesión
    @Test
    fun displays_login_button() {

        composeTestRule
            .onNodeWithText("Iniciar Sesión")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    // Scenario: 04-CPI Mostrar opción de continuar con Google
    @Test
    fun displays_google_login_button() {

        composeTestRule
            .onNodeWithText("Continuar con Google")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }
}

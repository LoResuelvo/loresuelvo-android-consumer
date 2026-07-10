package com.loresuelvo.consumer.acceptance.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
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
     * Hilt-injected singleton — THE SAME instance the activity's
     * `SessionViewModel` is observing. Pre-Fase 8 the tests
     * constructed a fresh `EncryptedAuthSessionStore` locally and
     * relied on the process-wide `SessionStateHolder` to keep the
     * in-memory graph in sync; with Fase 8 that shared state is
     * gone, so the tests have to mutate the live singleton directly
     * for `LoResuelvoNav` to pick the right route on `recreate()`.
     */
    @Inject
    lateinit var sessionStore: AuthSessionStore

    @Before
    fun setUp() {
        hiltRule.inject()
        sessionStore.clearSession()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
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
}

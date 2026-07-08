package com.loresuelvo.consumer.acceptance.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.auth.createEncryptedSessionPrefs
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)

class WelcomeScreenAcceptanceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        EncryptedAuthSessionStore(createEncryptedSessionPrefs(composeTestRule.activity)).clearSession()
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

package com.loresuelvo.consumer.acceptance.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.loresuelvo.consumer.MainActivity
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun welcome_screen_displays_main_elements() {

        composeTestRule
            .onNodeWithText("Registrarse")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText("Iniciar Sesión")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText("Continuar con Google")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}
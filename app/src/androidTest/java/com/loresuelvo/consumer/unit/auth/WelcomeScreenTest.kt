package com.loresuelvo.consumer.unit.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.loresuelvo.consumer.ui.screens.auth.WelcomeScreen
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcome_screen_displays_main_elements() {

        composeTestRule.setContent {
            WelcomeScreen()
        }

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

    @Test
    fun clicking_register_triggers_auth0_signup() {

        var registerClicked = false

        composeTestRule.setContent {

            WelcomeScreen(
                onRegisterClick = {
                    registerClicked = true
                }
            )
        }

        composeTestRule
            .onNodeWithText("Registrarse")
            .performClick()

        Assert.assertTrue(registerClicked)
    }

    @Test
    fun authenticated_user_sees_their_name() {

        composeTestRule.setContent {
            WelcomeScreen(
                authenticatedUserName = "Andres"
            )
        }

        composeTestRule
            .onNodeWithText("Andres")
            .assertIsDisplayed()
    }
}

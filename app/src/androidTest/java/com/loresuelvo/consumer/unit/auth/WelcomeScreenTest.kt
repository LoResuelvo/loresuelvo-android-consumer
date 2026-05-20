package com.loresuelvo.consumer.unit.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
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
    fun authenticated_user_sees_greeting_and_name_header() {

        composeTestRule.setContent {
            WelcomeScreen(
                authSession = AuthSession(
                    user = User(
                        displayName = "Andres",
                        firstName = "Andres",
                        lastName = "Colina",
                        email = "andy@pro.com"
                    ),
                    accessToken = "fake-token"
                )
            )
        }

        composeTestRule
            .onNodeWithText("Hola,")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Andres")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cerrar sesión")
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Registrarse")
            .assertCountEquals(0)
    }

    @Test
    fun displays_authentication_error_message() {

        composeTestRule.setContent {

            WelcomeScreen(
                errorMessage = "No pudimos completar el registro"
            )
        }

        composeTestRule
            .onNodeWithText("No pudimos completar el registro")
            .assertIsDisplayed()
    }
}

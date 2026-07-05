package com.loresuelvo.consumer.integration.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.loresuelvo.consumer.domain.auth.AuthProvider
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.ui.screens.auth.WelcomeScreen
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class WelcomeScreenIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clicking_register_calls_auth_provider_signup() {

        var signupCalled = false

        val fakeAuthProvider = object : AuthProvider {

            override fun signup() {
                signupCalled = true
            }

            override var onAuthenticated: (AuthSession) -> Unit = {}

            override var onAuthenticationError: (String) -> Unit = {}
        }

        composeTestRule.setContent {

            WelcomeScreen(
                onRegisterClick = {
                    fakeAuthProvider.signup()
                }
            )
        }

        composeTestRule
            .onNodeWithText("Registrarse")
            .performClick()

        Assert.assertTrue(signupCalled)
    }
}

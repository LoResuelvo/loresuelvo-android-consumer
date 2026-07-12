package com.loresuelvo.consumer.integration.auth

import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.screens.auth.WelcomeScreen
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented integration test for [WelcomeScreen]: asserts each
 * primary action forwards to its callback. The actual
 * `AuthProvider.signup(context)` invocation is exercised at the unit
 * level and via the `WelcomeViewModel` wiring in `LoResuelvoNav`;
 * this test stays focused on the click -> callback handoff.
 */
class WelcomeScreenIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(@StringRes resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(resourceId)

    @Test
    fun clicking_register_invokes_onRegisterClick() {

        var registerClicked = false

        composeTestRule.setContent {
            LoresuelvoTheme {
                WelcomeScreen(onRegisterClick = { registerClicked = true })
            }
        }

        composeTestRule
            .onNodeWithText(string(R.string.welcome_register))
            .performClick()

        Assert.assertTrue(registerClicked)
    }

    @Test
    fun clicking_login_invokes_onLoginClick() {

        var loginClicked = false

        composeTestRule.setContent {
            LoresuelvoTheme {
                WelcomeScreen(onLoginClick = { loginClicked = true })
            }
        }

        composeTestRule
            .onNodeWithText(string(R.string.welcome_login))
            .performClick()

        Assert.assertTrue(loginClicked)
    }

    @Test
    fun clicking_google_invokes_onGoogleClick() {

        var googleClicked = false

        composeTestRule.setContent {
            LoresuelvoTheme {
                WelcomeScreen(onGoogleClick = { googleClicked = true })
            }
        }

        composeTestRule
            .onNodeWithText(string(R.string.welcome_google))
            .performClick()

        Assert.assertTrue(googleClicked)
    }
}

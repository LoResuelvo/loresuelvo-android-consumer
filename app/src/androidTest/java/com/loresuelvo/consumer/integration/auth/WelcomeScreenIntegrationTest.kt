package com.loresuelvo.consumer.integration.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.loresuelvo.consumer.ui.screens.auth.WelcomeScreen
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented integration test for [WelcomeScreen]: clicks the
 * "Registrarse" button and asserts the `onRegisterClick` lambda
 * fires.
 *
 * The actual `AuthProvider.signup(context)` invocation is exercised
 * at the unit level by `Auth0AuthProviderTest` (in `src/test/`) and
 * via the `WelcomeViewModel` wiring in `LoResuelvoNav`. This test
 * stays focused on the click → callback handoff of the Composable.
 */
class WelcomeScreenIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clicking_register_invokes_onRegisterClick() {

        var registerClicked = false

        composeTestRule.setContent {
            WelcomeScreen(
                onRegisterClick = { registerClicked = true }
            )
        }

        composeTestRule
            .onNodeWithText("Registrarse")
            .performClick()

        Assert.assertTrue(registerClicked)
    }
}

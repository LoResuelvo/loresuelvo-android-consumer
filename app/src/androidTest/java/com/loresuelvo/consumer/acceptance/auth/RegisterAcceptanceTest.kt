package com.loresuelvo.consumer.acceptance.auth

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasDataString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.BuildConfig
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.data.auth.SharedPreferencesAuthSessionStore
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.ui.screens.auth.WelcomeScreen
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegisterWithAuth0AcceptanceTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        Intents.init()
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        )
        SharedPreferencesAuthSessionStore(composeTestRule.activity).clearSession()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    @After
    fun tearDown() {
        SharedPreferencesAuthSessionStore(composeTestRule.activity).clearSession()
        Intents.release()
    }

    // Scenario: 01-RCN Redirección al portal de registro de Auth0
    @Ignore("Fails in CI because Auth0 launches external activity")
    @Test
    fun redirects_to_auth0_signup() {

        composeTestRule
            .onNodeWithText("Registrarse")
            .assertHasClickAction()
            .performClick()

        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasDataString(containsString(BuildConfig.AUTH0_DOMAIN)),
                hasDataString(containsString("/authorize")),
                hasDataString(containsString("screen_hint=signup"))
            )
        )
    }

    // Scenario: 02-RCN Registro exitoso
    @Test
    fun register_successfully_with_auth0() {

        mockAuthenticatedUser("Andres")

        composeTestRule
            .onNodeWithText("Andres")
            .assertIsDisplayed()
    }

    // Scenario: 03-RCN Verificación de sesión persistente
    @Test
    fun keeps_authenticated_session() {

        persistAuthenticatedUser("Andres Colina")

        composeTestRule
            .onNodeWithText("Cerrar sesión")
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Registrarse")
            .assertCountEquals(0)
    }

    // Scenario: 04-RCN Registro fallido
    @Test
    fun register_failure_with_auth0() {

        composeTestRule.runOnUiThread {

            composeTestRule.activity.setContent {

                WelcomeScreen(
                    errorMessage = "No pudimos completar el registro"
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("No pudimos completar el registro")
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Andres")
            .assertCountEquals(0)
    }

    private fun mockAuthenticatedUser(
        name: String
    ) {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.setContent {
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
        }
        composeTestRule.waitForIdle()
    }

    private fun persistAuthenticatedUser(
        name: String
    ) {
        composeTestRule.runOnUiThread {
            SharedPreferencesAuthSessionStore(composeTestRule.activity).saveSession(
                AuthSession(
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

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    private fun mockUnauthenticatedUser() {
        // TODO:
        // Mockear sesión cancelada o inválida
    }

}

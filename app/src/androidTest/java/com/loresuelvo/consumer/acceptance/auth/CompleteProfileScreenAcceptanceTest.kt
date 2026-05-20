package com.loresuelvo.consumer.acceptance.auth

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.loresuelvo.consumer.ui.screens.auth.CompleteProfileScreen
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.loresuelvo.consumer.MainActivity
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.loresuelvo.consumer.data.auth.SharedPreferencesAuthSessionStore
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText

@RunWith(AndroidJUnit4::class)

class CompleteProfileScreenAcceptanceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Scenario: 01-CPC Mostrar formulario de completar perfil
    @Test
    fun displays_complete_profile_form() {

        persistIncompleteAuthenticatedUser()

        composeTestRule
            .onNodeWithText("Completa tu perfil")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Nombre")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Apellido")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Continuar")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    // Scenario: 02-CPC Completar perfil exitosamente
    @Test
    fun completes_profile_successfully() {

        persistIncompleteAuthenticatedUser()

        composeTestRule
            .onNodeWithTag("first-name")
            .performTextInput("Andres")

        composeTestRule
            .onNodeWithTag("last-name")
            .performTextInput("Colina")

        composeTestRule
            .onNodeWithText("Continuar")
            .performClick()

        composeTestRule
            .onNodeWithText("Hola,")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Andres")
            .assertIsDisplayed()
    }

    // Scenario: 03-CPC Nombre obligatorio
    @Test
    fun requires_first_name() {

        persistIncompleteAuthenticatedUser()

        composeTestRule
            .onNodeWithTag("last-name")
            .performTextInput("Colina")

        composeTestRule
            .onNodeWithText("Continuar")
            .performClick()

        composeTestRule
            .onNodeWithText("El nombre es obligatorio")
            .assertIsDisplayed()
    }

    // Scenario: 04-CPC Apellido obligatorio
    @Test
    fun requires_last_name() {

        persistIncompleteAuthenticatedUser()

        composeTestRule
            .onNodeWithTag("first-name")
            .performTextInput("Andres")

        composeTestRule
            .onNodeWithText("Continuar")
            .performClick()

        composeTestRule
            .onNodeWithText("El apellido es obligatorio")
            .assertIsDisplayed()
    }

    // Scenario: 05-CPC Persistir perfil completado
    @Test
    fun keeps_completed_profile_after_reopening_app() {

        persistIncompleteAuthenticatedUser()

        composeTestRule
            .onNodeWithTag("first-name")
            .performTextInput("Andres")

        composeTestRule
            .onNodeWithTag("last-name")
            .performTextInput("Colina")

        composeTestRule
            .onNodeWithText("Continuar")
            .performClick()

        composeTestRule
            .activityRule
            .scenario
            .recreate()

        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithText("Completa tu perfil")
            .assertCountEquals(0)

        composeTestRule
            .onNodeWithText("Hola,")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Andres")
            .assertIsDisplayed()
    }

    private fun persistIncompleteAuthenticatedUser() {

        composeTestRule.runOnUiThread {

            SharedPreferencesAuthSessionStore(
                composeTestRule.activity
            ).saveSession(
                AuthSession(
                    user = User(
                        displayName = "Andres",
                        firstName = null,
                        lastName = null,
                        email = "andy@pro.com"
                    ),
                    accessToken = "fake-token"
                )
            )
        }

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }
}
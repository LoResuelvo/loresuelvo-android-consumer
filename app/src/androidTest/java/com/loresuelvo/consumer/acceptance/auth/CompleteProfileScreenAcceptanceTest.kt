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
    @Ignore("Peding")
    // Scenario: 03-CPC Nombre obligatorio
    @Test
    fun requires_first_name() {
        // Given que estoy autenticado
        // And mi perfil está incompleto
        // When dejo vacío el campo "Nombre"
        // And ingreso "Colina" como apellido
        // And presiono "Continuar"
        // Then veo el mensaje "El nombre es obligatorio"
    }
    @Ignore("Peding")
    // Scenario: 04-CPC Apellido obligatorio
    @Test
    fun requires_last_name() {
        // Given que estoy autenticado
        // And mi perfil está incompleto
        // When ingreso "Andres" como nombre
        // And dejo vacío el campo "Apellido"
        // And presiono "Continuar"
        // Then veo el mensaje "El apellido es obligatorio"
    }
    @Ignore("Peding")
    // Scenario: 05-CPC Persistir perfil completado
    @Test
    fun keeps_completed_profile_after_reopening_app() {
        // Given que completé mi perfil correctamente
        // When vuelvo a abrir la aplicación
        // Then no veo la pantalla "Completar perfil"
        // And veo la pantalla principal
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
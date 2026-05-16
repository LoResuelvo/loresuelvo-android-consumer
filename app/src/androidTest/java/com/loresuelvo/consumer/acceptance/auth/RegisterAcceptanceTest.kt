package com.loresuelvo.consumer.acceptance.auth

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegisterWithAuth0AcceptanceTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Scenario: 01-RCN Redirección al portal de registro de Auth0
    @Test
    fun redirects_to_auth0_signup() {

        composeTestRule
            .onNodeWithText("Registrarse")
            .assertHasClickAction()
            .performClick()

        // TODO:
        // Validar apertura de Universal Login
    }

    // Scenario: 02-RCN Registro exitoso
    @Test
    @Ignore("Pending scenario 02-RCN")
    fun register_successfully_with_auth0() {

        mockAuthenticatedUser("Andres")

        composeTestRule
            .onNodeWithText("Andres")
            .assertIsDisplayed()
    }

    // Scenario: 03-RCN Verificación de sesión persistente
    @Test
    @Ignore("Pending scenario 03-RCN")
    fun keeps_authenticated_session() {

        mockAuthenticatedUser("Andres Colina")

        composeTestRule
            .onNodeWithText("Cerrar sesión")
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Registrarse")
            .assertCountEquals(0)
    }

    // Scenario: 04-RCN Registro fallido
    @Test
    @Ignore("Pending scenario 04-RCN")
    fun register_failure_with_auth0() {

        mockUnauthenticatedUser()

        composeTestRule
            .onAllNodesWithText("Andres")
            .assertCountEquals(0)
    }

    private fun mockAuthenticatedUser(
        name: String
    ) {
        // TODO:
        // Mockear sesión Auth0 válida
    }

    private fun mockUnauthenticatedUser() {
        // TODO:
        // Mockear sesión cancelada o inválida
    }

}

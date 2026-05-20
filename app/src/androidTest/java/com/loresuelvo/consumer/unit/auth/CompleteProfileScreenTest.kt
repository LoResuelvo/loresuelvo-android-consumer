package com.loresuelvo.consumer.unit.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.loresuelvo.consumer.ui.screens.auth.CompleteProfileScreen
import org.junit.Rule
import org.junit.Test

class CompleteProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_complete_profile_form() {

        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "",
                lastName = "",
                errorMessage = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onContinueClick = {}
            )
        }

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

    @Test
    fun displays_validation_error_message() {

        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "",
                lastName = "",
                errorMessage = "El nombre es obligatorio",
                onFirstNameChange = {},
                onLastNameChange = {},
                onContinueClick = {}
            )
        }

        composeTestRule
            .onNodeWithText("El nombre es obligatorio")
            .assertIsDisplayed()
    }
}
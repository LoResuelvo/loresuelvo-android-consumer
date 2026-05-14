package com.loresuelvo.consumer.acceptance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Ignore

@RunWith(AndroidJUnit4::class)
@Ignore("Por implementar")
class RegisterDuplicateEmailAcceptance {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Rule: No se puede registrar con un correo electrónico que ya esté en uso
    // Scenario: 05-RCN Rechazar un registro con correo ya existente
    @Test
    fun show_error_when_email_is_already_registered() {
        // Nota: El "Given" se implementará más adelante inyectando un estado al sistema

        fillForm(name = "Carla", last = "Gomez", email = "carla@example.com")
        composeTestRule.onNodeWithTag("register_button").performClick()

        composeTestRule.onNodeWithText("El correo electrónico ya está registrado").assertIsDisplayed()
    }

    private fun fillForm(name: String, last: String, email: String) {
        composeTestRule.onNodeWithTag("name_input").performTextInput(name)
        composeTestRule.onNodeWithTag("lastname_input").performTextInput(last)
        composeTestRule.onNodeWithTag("email_input").performTextInput(email)
        composeTestRule.onNodeWithTag("password_input").performTextInput("Segura12345?")
    }
}
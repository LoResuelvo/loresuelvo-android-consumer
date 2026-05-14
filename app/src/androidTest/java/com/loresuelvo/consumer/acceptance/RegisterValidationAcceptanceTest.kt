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
class RegisterValidationAcceptance {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // --- Rule: El correo electrónico debe tener un formato válido ---

    // Scenario: 02-RCN Rechazar un registro con correo sin @
    @Test
    fun show_error_when_email_missing_at() {
        fillForm(email = "anaexample.com")
        clickRegister()
        composeTestRule.onNodeWithText("El formato del correo es inválido").assertIsDisplayed()
    }

    // Scenario: 03-RCN Rechazar un registro con correo sin dominio
    @Test
    fun show_error_when_email_missing_domain() {
        fillForm(email = "ana@")
        clickRegister()
        composeTestRule.onNodeWithText("El formato del correo es inválido").assertIsDisplayed()
    }

    // Scenario: 04-RCN Rechazar un registro con correo sin nombre de usuario
    @Test
    fun show_error_when_email_missing_username() {
        fillForm(email = "@example.com")
        clickRegister()
        composeTestRule.onNodeWithText("El formato del correo es inválido").assertIsDisplayed()
    }

    // --- Rule: La contraseña debe contener entre 12 y 64 caracteres ---

    // Scenario: 03-RCN (Password) Rechazar registro con contraseña menor a 12 caracteres
    @Test
    fun show_error_when_password_is_too_short() {
        fillForm(pass = "abc123")
        clickRegister()
        composeTestRule.onNodeWithText("La contraseña es demasiado corta").assertIsDisplayed()
    }

    // --- Rule: La contraseña debe incluir mayúscula, minúscula, número y carácter especial ---

    // Scenario: 04-RCN (Password) Rechazar registro sin mayúscula
    @Test
    fun show_error_when_password_missing_uppercase() {
        fillForm(pass = "abc123456789!")
        clickRegister()
        composeTestRule.onNodeWithText("La contraseña es insegura").assertIsDisplayed()
    }

    // Scenario: 05-RCN (Password) Rechazar registro sin minúscula
    @Test
    fun show_error_when_password_missing_lowercase() {
        fillForm(pass = "ABC123456789!")
        clickRegister()
        composeTestRule.onNodeWithText("La contraseña es insegura").assertIsDisplayed()
    }

    // Scenario: 06-RCN (Password) Rechazar registro sin número
    @Test
    fun show_error_when_password_missing_number() {
        fillForm(pass = "aaaaaaasdsadfsaf!")
        clickRegister()
        composeTestRule.onNodeWithText("La contraseña es insegura").assertIsDisplayed()
    }

    private fun clickRegister() = composeTestRule.onNodeWithTag("register_button").performClick()

    private fun fillForm(name: String = "Ana", last: String = "Perez", email: String = "ana@example.com", pass: String = "Segura12345?") {
        composeTestRule.onNodeWithTag("name_input").performTextInput(name)
        composeTestRule.onNodeWithTag("lastname_input").performTextInput(last)
        composeTestRule.onNodeWithTag("email_input").performTextInput(email)
        composeTestRule.onNodeWithTag("password_input").performTextInput(pass)
    }
}
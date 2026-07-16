package com.loresuelvo.consumer.ui.screens.profile

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI tests for [CompleteProfileScreen]. Run on the JVM via
 * Robolectric so the suite stays in `src/test/` and executes in
 * the standard `make test` flow (no emulator). The old suite that
 * lived in `androidTest/unit/auth/` was renamed to
 * `androidTest/.../acceptance/auth/` territory by mistake; this
 * is the canonical location per AGENTS.md.
 *
 * The test resource set (default = `values/strings.xml`, es-AR)
 * is what the screen reads, so the asserted strings are in Spanish.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class CompleteProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_complete_profile_form() {
        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "",
                lastName = "",
                loading = false,
                error = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onContinueClick = {},
                onEvent = {},
            )
        }

        // assertExists (not assertIsDisplayed) so the assertion does
        // not depend on the small Robolectric viewport; we only care
        // that the labels are present in the tree.
        composeTestRule.onNodeWithText("Ya casi estamos.").assertExists()
        composeTestRule.onNodeWithText("Nombre").assertExists()
        composeTestRule.onNodeWithText("Apellido").assertExists()
        composeTestRule.onNodeWithText("Continuar")
            .assertExists()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun shows_typed_MissingFirstName_error_message() {
        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "",
                lastName = "Colina",
                loading = false,
                error = CompleteProfileError.MissingFirstName,
                onFirstNameChange = {},
                onLastNameChange = {},
                onContinueClick = {},
                onEvent = {},
            )
        }

        composeTestRule
            .onNodeWithText("El nombre es obligatorio")
            .assertIsDisplayed()
    }

    @Test
    fun shows_typed_MissingLastName_error_message() {
        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "Andres",
                lastName = "",
                loading = false,
                error = CompleteProfileError.MissingLastName,
                onFirstNameChange = {},
                onLastNameChange = {},
                onContinueClick = {},
                onEvent = {},
            )
        }

        composeTestRule
            .onNodeWithText("El apellido es obligatorio")
            .assertIsDisplayed()
    }

    @Test
    fun shows_typed_Server_error_message_with_code_and_text() {
        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "Andres",
                lastName = "Colina",
                loading = false,
                error = CompleteProfileError.Server(
                    code = 409,
                    message = "Email is already registered",
                ),
                onFirstNameChange = {},
                onLastNameChange = {},
                onContinueClick = {},
                onEvent = {},
            )
        }

        composeTestRule
            .onNodeWithText(
                "No pudimos completar el registro (409). Email is already registered"
            )
            .assertIsDisplayed()
    }

    @Test
    fun disable_continue_button_while_loading() {
        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "Andres",
                lastName = "Colina",
                loading = true,
                error = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onContinueClick = {},
                onEvent = {},
            )
        }

        composeTestRule
            .onNodeWithText("Continuar")
            .assertExists()
            .assertIsNotEnabled()
    }

    @Test
    fun click_on_continue_invokes_callback() {
        var clicked = 0
        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "Andres",
                lastName = "Colina",
                loading = false,
                error = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onContinueClick = { clicked += 1 },
                onEvent = {},
            )
        }

        composeTestRule
            .onNodeWithText("Continuar")
            .performClick()

        org.junit.Assert.assertEquals(1, clicked)
    }

    @Test
    fun typing_in_first_name_invokes_callback() {
        var captured = ""
        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "",
                lastName = "",
                loading = false,
                error = null,
                onFirstNameChange = { captured = it },
                onLastNameChange = {},
                onContinueClick = {},
                onEvent = {},
            )
        }

        composeTestRule
            .onNodeWithTag("first-name")
            .performTextInput("Andres")

        assert(captured == "Andres")
    }
}

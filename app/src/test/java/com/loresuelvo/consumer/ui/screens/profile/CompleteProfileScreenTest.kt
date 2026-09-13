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
 * `androidTest/.../instrumented/auth/` territory by mistake; this
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
                street = "Tucuman",
                streetNumber = "123",
                floor = "",
                unit = "",
                loading = false,
                error = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onContinueClick = {},
                onStreetChange = {},
                onStreetNumberChange = {},
                onFloorChange = {},
                onUnitChange = {},
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
                street = "Tucuman",
                streetNumber = "123",
                floor = "1",
                unit = "A",
                loading = false,
                error = CompleteProfileError.MissingFirstName,
                onFirstNameChange = {},
                onLastNameChange = {},
                onContinueClick = {},
                onStreetChange = {},
                onStreetNumberChange = {},
                onFloorChange = {},
                onUnitChange = {},
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
                street = "Tucuman",
                streetNumber = "123",
                floor = "1",
                unit = "A",
                loading = false,
                error = CompleteProfileError.MissingLastName,
                onFirstNameChange = {},
                onLastNameChange = {},
                onStreetChange = {},
                onStreetNumberChange = {},
                onFloorChange = {},
                onUnitChange = {},
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
                street = "Tucuman",
                streetNumber = "123",
                floor = "1",
                unit = "A",
                loading = false,
                error = CompleteProfileError.Server(
                    code = 409,
                    message = "Email is already registered",
                ),
                onFirstNameChange = {},
                onLastNameChange = {},
                onStreetChange = {},
                onStreetNumberChange = {},
                onFloorChange = {},
                onUnitChange = {},
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
                street = "Tucuman",
                streetNumber = "123",
                floor = "1",
                unit = "A",
                loading = true,
                error = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onContinueClick = {},
                onStreetChange = {},
                onStreetNumberChange = {},
                onFloorChange = {},
                onUnitChange = {},
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
                street = "Tucuman",
                streetNumber = "123",
                floor = "1",
                unit = "A",
                loading = false,
                error = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onContinueClick = { clicked += 1 },
                onStreetChange = {},
                onStreetNumberChange = {},
                onFloorChange = {},
                onUnitChange = {},
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
                street = "Tucuman",
                streetNumber = "123",
                floor = "1",
                unit = "A",
                loading = false,
                error = null,
                onFirstNameChange = { captured = it },
                onLastNameChange = {},
                onContinueClick = {},
                onStreetChange = {},
                onStreetNumberChange = {},
                onFloorChange = {},
                onUnitChange = {},
                onEvent = {},
            )
        }

        composeTestRule
            .onNodeWithTag("first-name")
            .performTextInput("Andres")

        assert(captured == "Andres")
    }

    @Test
    fun displays_address_fields() {
        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "Andres",
                lastName = "Colina",
                street = "Tucuman",
                streetNumber = "123",
                floor = "",
                unit = "",
                loading = false,
                error = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onStreetChange = {},
                onStreetNumberChange = {},
                onFloorChange = {},
                onUnitChange = {},
                onContinueClick = {},
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText("Calle").assertExists()
        composeTestRule.onNodeWithText("Número").assertExists()
        composeTestRule.onNodeWithText("Piso (opcional)").assertExists()
        composeTestRule.onNodeWithText("Unidad (opcional)").assertExists()
    }

    @Test
    fun typing_in_street_invokes_callback() {
        var captured = ""

        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "",
                lastName = "",
                street = "",
                streetNumber = "",
                floor = "",
                unit = "",
                loading = false,
                error = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onStreetChange = { captured = it },
                onStreetNumberChange = {},
                onFloorChange = {},
                onUnitChange = {},
                onContinueClick = {},
                onEvent = {},
            )
        }

        composeTestRule
            .onNodeWithTag("street")
            .performTextInput("Avellaneda")

        assert(captured == "Avellaneda")
    }

    @Test
    fun typing_in_street_number_invokes_callback() {
        var captured = ""

        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "",
                lastName = "",
                street = "",
                streetNumber = "",
                floor = "",
                unit = "",
                loading = false,
                error = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onStreetChange = {},
                onStreetNumberChange = { captured = it },
                onFloorChange = {},
                onUnitChange = {},
                onContinueClick = {},
                onEvent = {},
            )
        }

        composeTestRule
            .onNodeWithTag("street-number")
            .performTextInput("456")

        assert(captured == "456")
    }

    @Test   
    fun typing_in_floor_invokes_callback() {
        var captured = ""

        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "",
                lastName = "",
                street = "",
                streetNumber = "",
                floor = "",
                unit = "",
                loading = false,
                error = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onStreetChange = {},
                onStreetNumberChange = {},
                onFloorChange = { captured = it },
                onUnitChange = {},
                onContinueClick = {},
                onEvent = {},
            )
        }

        composeTestRule
            .onNodeWithTag("floor")
            .performTextInput("2")

        assert(captured == "2")
    }

    @Test
    fun typing_in_unit_invokes_callback() {
        var captured = ""

        composeTestRule.setContent {
            CompleteProfileScreen(
                firstName = "",
                lastName = "",
                street = "",
                streetNumber = "",
                floor = "",
                unit = "",
                loading = false,
                error = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onStreetChange = {},
                onStreetNumberChange = {},
                onFloorChange = {},
                onUnitChange = { captured = it },
                onContinueClick = {},
                onEvent = {},
            )
        }

        composeTestRule
            .onNodeWithTag("unit")
            .performTextInput("B")

     
            assert(captured == "B")
    }
}

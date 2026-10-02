package com.loresuelvo.consumer.ui.screens.profile

import android.graphics.Bitmap
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

/**
 * UI tests for [CompleteProfileScreen]. The screen boundary is intentionally
 * exercised through one state object and one action dispatcher, matching the
 * production contract used by [CompleteProfileViewModel].
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class CompleteProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displays_complete_profile_form() {
        setProfileContent(
            state = CompleteProfileUiState(
                street = "Tucuman",
                streetNumber = "123",
            ),
        )

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
        setProfileContent(
            state = completeState(error = CompleteProfileError.MissingFirstName),
        )

        composeTestRule.onNodeWithText("El nombre es obligatorio").assertIsDisplayed()
    }

    @Test
    fun shows_typed_MissingLastName_error_message() {
        setProfileContent(
            state = completeState(
                firstName = "Andres",
                error = CompleteProfileError.MissingLastName,
            ),
        )

        composeTestRule.onNodeWithText("El apellido es obligatorio").assertIsDisplayed()
    }

    @Test
    fun shows_typed_Server_error_message_with_code_and_text() {
        setProfileContent(
            state = completeState(
                error = CompleteProfileError.Server(
                    code = 409,
                    message = "Email is already registered",
                ),
            ),
        )

        composeTestRule
            .onNodeWithText(
                "No pudimos completar el registro (409). Email is already registered",
            )
            .assertIsDisplayed()
    }

    @Test
    fun disable_continue_button_while_loading() {
        setProfileContent(state = completeState(loading = true))

        composeTestRule.onNodeWithText("Continuar")
            .assertExists()
            .assertIsNotEnabled()
    }

    @Test
    fun click_on_continue_dispatches_action() {
        var action: CompleteProfileAction? = null
        setProfileContent(
            state = completeState(),
            onAction = { action = it },
        )

        composeTestRule.onNodeWithText("Continuar").performClick()

        assertEquals(CompleteProfileAction.ContinueClicked, action)
    }

    @Test
    fun typing_in_first_name_dispatches_action() {
        var action: CompleteProfileAction? = null
        setProfileContent(onAction = { action = it })

        composeTestRule.onNodeWithTag("first-name").performTextInput("Andres")

        assertEquals(CompleteProfileAction.FirstNameChanged("Andres"), action)
    }

    @Test
    fun displays_address_fields() {
        setProfileContent(
            state = completeState(
                firstName = "Andres",
                lastName = "Colina",
                floor = "",
                unit = "",
            ),
        )

        composeTestRule.onNodeWithText("Calle").assertExists()
        composeTestRule.onNodeWithText("Número").assertExists()
        composeTestRule.onNodeWithText("Piso (opcional)").assertExists()
        composeTestRule.onNodeWithText("Unidad (opcional)").assertExists()
    }

    @Test
    fun pending_profile_photo_shows_circular_crop_editor() {
        setProfileContent(
            state = completeState().copy(
                pendingProfilePhoto = MediaUpload.Image(
                    bytes = testPhotoBytes(),
                    mimeType = "image/jpeg",
                    originalName = "avatar.jpg",
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("Ajustá tu foto")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("profile-photo-crop-viewport")
            .assertExists()
    }

    @Test
    fun typing_in_street_dispatches_action() {
        var action: CompleteProfileAction? = null
        setProfileContent(
            state = completeState(street = ""),
            onAction = { action = it },
        )

        composeTestRule.onNodeWithTag("street").performTextInput("Avellaneda")

        assertEquals(CompleteProfileAction.StreetChanged("Avellaneda"), action)
    }

    @Test
    fun typing_in_street_number_dispatches_action() {
        var action: CompleteProfileAction? = null
        setProfileContent(
            state = completeState(street = "", streetNumber = ""),
            onAction = { action = it },
        )

        composeTestRule.onNodeWithTag("street-number").performTextInput("456")

        assertEquals(CompleteProfileAction.StreetNumberChanged("456"), action)
    }

    @Test
    fun typing_in_floor_dispatches_action() {
        var action: CompleteProfileAction? = null
        setProfileContent(
            state = completeState(street = "", streetNumber = "", floor = ""),
            onAction = { action = it },
        )

        composeTestRule.onNodeWithTag("floor").performTextInput("2")

        assertEquals(CompleteProfileAction.FloorChanged("2"), action)
    }

    @Test
    fun typing_in_unit_dispatches_action() {
        var action: CompleteProfileAction? = null
        setProfileContent(
            state = completeState(street = "", streetNumber = "", floor = "", unit = ""),
            onAction = { action = it },
        )

        composeTestRule.onNodeWithTag("unit").performTextInput("B")

        assertEquals(CompleteProfileAction.UnitChanged("B"), action)
    }

    private fun setProfileContent(
        state: CompleteProfileUiState = completeState(),
        onAction: (CompleteProfileAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompleteProfileScreen(
                state = state,
                onAction = onAction,
            )
        }
    }

    private fun completeState(
        firstName: String = "",
        lastName: String = "",
        street: String = "Tucuman",
        streetNumber: String = "123",
        floor: String = "1",
        unit: String = "A",
        loading: Boolean = false,
        error: CompleteProfileError? = null,
    ): CompleteProfileUiState = CompleteProfileUiState(
        firstName = firstName,
        lastName = lastName,
        street = street,
        streetNumber = streetNumber,
        floor = floor,
        unit = unit,
        loading = loading,
        error = error,
    )

    private fun testPhotoBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(160, 100, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
            bitmap.recycle()
            output.toByteArray()
        }
    }
}

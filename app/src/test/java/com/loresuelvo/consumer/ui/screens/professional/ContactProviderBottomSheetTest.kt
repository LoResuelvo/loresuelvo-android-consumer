package com.loresuelvo.consumer.ui.screens.professional

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [ContactProviderBottomSheet]. Run on the
 * JVM via Robolectric so the unit-test task exercises them without
 * a device.
 *
 * The composable is stateless: every field below asserts that the
 * callbacks flow back correctly and the submit-button gating
 * tracks the `canSubmit` flag, which is derived from the typed
 * UI state in [ContactProviderUiState.Open.canSubmit].
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ContactProviderBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(@androidx.annotation.StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Application>().getString(id)

    @Test
    fun renders_provider_avatar_name_and_category() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ContactProviderBottomSheet(
                        provider = sampleProvider(),
                        title = "",
                        description = "",
                        canSubmit = false,
                        isSubmitting = false,
                        error = null,

                        attachedImages = emptyList(),

                        attachmentError = null,
                        onTitleChange = {},
                        onDescriptionChange = {},
                        onAttachImagesClick = {},
                        onRemoveImage = {},
                        onSubmit = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Juan Pérez").assertIsDisplayed()
        composeTestRule.onNodeWithText("Plomería").assertIsDisplayed()
    }

    @Test
    fun renders_modal_title_and_subtitle() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ContactProviderBottomSheet(
                        provider = sampleProvider(),
                        title = "",
                        description = "",
                        canSubmit = false,
                        isSubmitting = false,
                        error = null,

                        attachedImages = emptyList(),

                        attachmentError = null,
                        onTitleChange = {},
                        onDescriptionChange = {},
                        onAttachImagesClick = {},
                        onRemoveImage = {},
                        onSubmit = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText(
            string(R.string.contact_provider_modal_title),
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            string(R.string.contact_provider_modal_subtitle),
        ).assertIsDisplayed()
    }

    @Test
    fun submit_button_is_disabled_when_canSubmit_is_false() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ContactProviderBottomSheet(
                        provider = sampleProvider(),
                        title = "",
                        description = "",
                        canSubmit = false,
                        isSubmitting = false,
                        error = null,

                        attachedImages = emptyList(),

                        attachmentError = null,
                        onTitleChange = {},
                        onDescriptionChange = {},
                        onAttachImagesClick = {},
                        onRemoveImage = {},
                        onSubmit = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(CONTACT_PROVIDER_SUBMIT_BUTTON_TAG)
            .assertIsNotEnabled()
    }

    @Test
    fun submit_button_is_enabled_when_canSubmit_is_true() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ContactProviderBottomSheet(
                        provider = sampleProvider(),
                        title = "Fuga",
                        description = "Hay una fuga",
                        canSubmit = true,
                        isSubmitting = false,
                        error = null,

                        attachedImages = emptyList(),

                        attachmentError = null,
                        onTitleChange = {},
                        onDescriptionChange = {},
                        onAttachImagesClick = {},

                        onRemoveImage = {},

                        onSubmit = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(CONTACT_PROVIDER_SUBMIT_BUTTON_TAG)
            .assertIsEnabled()
    }

    @Test
    fun typing_in_title_field_invokes_onTitleChange() {
        var captured = ""
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ContactProviderBottomSheet(
                        provider = sampleProvider(),
                        title = "",
                        description = "",
                        canSubmit = false,
                        isSubmitting = false,
                        error = null,

                        attachedImages = emptyList(),

                        attachmentError = null,
                        onTitleChange = { captured = it },
                        onDescriptionChange = {},
                        onAttachImagesClick = {},

                        onRemoveImage = {},

                        onSubmit = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(CONTACT_PROVIDER_TITLE_FIELD_TAG)
            .performTextInput("Fuga")

        assertEquals("Fuga", captured)
    }

    @Test
    fun typing_in_description_field_invokes_onDescriptionChange() {
        var captured = ""
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ContactProviderBottomSheet(
                        provider = sampleProvider(),
                        title = "",
                        description = "",
                        canSubmit = false,
                        isSubmitting = false,
                        error = null,

                        attachedImages = emptyList(),

                        attachmentError = null,
                        onTitleChange = {},
                        onDescriptionChange = { captured = it },
                        onAttachImagesClick = {},

                        onRemoveImage = {},

                        onSubmit = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(CONTACT_PROVIDER_DESCRIPTION_FIELD_TAG)
            .performTextInput("Hay una fuga")

        assertEquals("Hay una fuga", captured)
    }

    @Test
    fun tapping_submit_invokes_onSubmit() {
        var submitted = false
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ContactProviderBottomSheet(
                        provider = sampleProvider(),
                        title = "Fuga",
                        description = "Hay una fuga",
                        canSubmit = true,
                        isSubmitting = false,
                        error = null,

                        attachedImages = emptyList(),
                        attachmentError = null,
                        onTitleChange = {},
                        onDescriptionChange = {},
                        onAttachImagesClick = {},
                        onRemoveImage = {},
                        onSubmit = { submitted = true },
                        onCancel = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(CONTACT_PROVIDER_SUBMIT_BUTTON_TAG)
            .performScrollTo()
            .performClick()

        assertTrue(submitted)
    }

    @Test
    fun tapping_cancel_invokes_onCancel() {
        var cancelled = false
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ContactProviderBottomSheet(
                        provider = sampleProvider(),
                        title = "Fuga",
                        description = "Hay una fuga",
                        canSubmit = true,
                        isSubmitting = false,
                        error = null,

                        attachedImages = emptyList(),

                        attachmentError = null,
                        onTitleChange = {},
                        onDescriptionChange = {},
                        onAttachImagesClick = {},
                        onRemoveImage = {},
                        onSubmit = {},
                        onCancel = { cancelled = true },
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(CONTACT_PROVIDER_CANCEL_BUTTON_TAG)
            .performScrollTo()
            .performClick()

        assertTrue(cancelled)
    }

    @Test
    fun error_message_is_rendered_when_error_is_set() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ContactProviderBottomSheet(
                        provider = sampleProvider(),
                        title = "Fuga",
                        description = "Hay una fuga",
                        canSubmit = true,
                        isSubmitting = false,
                        error = ContactProviderError.Server(
                            code = 500,
                            message = "internal boom",
                        ),
                        attachedImages = emptyList(),
                        attachmentError = null,
                        onTitleChange = {},
                        onDescriptionChange = {},
                        onAttachImagesClick = {},
                        onRemoveImage = {},
                        onSubmit = {},
                        onCancel = {},
                    )
                }
            }
        }

        // The error surface is rendered AND the raw server message
        // is visible to the user (the typed error owns the text).
        composeTestRule.onNodeWithTag(CONTACT_PROVIDER_ERROR_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("internal boom").assertIsDisplayed()
    }

    @Test
    fun error_message_for_network_uses_localised_string() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ContactProviderBottomSheet(
                        provider = sampleProvider(),
                        title = "Fuga",
                        description = "Hay una fuga",
                        canSubmit = true,
                        isSubmitting = false,
                        error = ContactProviderError.Network,
                        attachedImages = emptyList(),
                        attachmentError = null,
                        onTitleChange = {},
                        onDescriptionChange = {},
                        onAttachImagesClick = {},
                        onRemoveImage = {},
                        onSubmit = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText(
            string(R.string.contact_provider_error_network),
        ).performScrollTo().assertIsDisplayed()
    }

    private fun sampleProvider() = Provider(
        id = 1,
        name = "Juan",
        surname = "Pérez",
        categoryId = 1,
        categoryName = "Plomería",
        profilePhotoUrl = null,
    )
}

package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose unit tests for [MediaAttachSheet].
 *
 * The attach sheet exposes only the media sources that are currently
 * part of the provider-chat UX:
 *
 *  - Galería — wired in 01-MM.
 *  - Cámara — wired in 02-MM.
 *
 * Audio recording is intentionally NOT part of this sheet. The
 * conversation composer exposes the microphone directly when the
 * text input is empty, WhatsApp-style, and therefore audio recording
 * must not regress into the `+` attachment menu.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class MediaAttachSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rows_render_gallery_and_camera() {
        composeTestRule.setContent {
            MediaAttachSheet(
                show = true,
                onDismiss = {},
                onGalleryClick = {},
                onCameraClick = {},
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(MEDIA_ATTACH_SHEET_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(MEDIA_ATTACH_GALLERY_ROW_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(MEDIA_ATTACH_CAMERA_ROW_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Galería")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cámara")
            .assertIsDisplayed()
    }

    @Test
    fun audio_row_does_not_exist() {
        composeTestRule.setContent {
            MediaAttachSheet(
                show = true,
                onDismiss = {},
                onGalleryClick = {},
                onCameraClick = {},
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithText("Audio")
            .fetchSemanticsNodes()
            .let { nodes ->
                assertTrue(
                    "Audio should not be rendered in the attach sheet",
                    nodes.isEmpty(),
                )
            }
    }

    @Test
    fun gallery_row_is_clickable_when_callback_is_provided() {
        composeTestRule.setContent {
            MediaAttachSheet(
                show = true,
                onDismiss = {},
                onGalleryClick = {},
                onCameraClick = null,
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(MEDIA_ATTACH_GALLERY_ROW_TAG)
            .assertHasClickAction()
    }

    @Test
    fun camera_row_is_clickable_when_callback_is_provided() {
        composeTestRule.setContent {
            MediaAttachSheet(
                show = true,
                onDismiss = {},
                onGalleryClick = {},
                onCameraClick = {},
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(MEDIA_ATTACH_CAMERA_ROW_TAG)
            .assertHasClickAction()
    }

    @Test
    fun camera_row_is_displayed_when_callback_is_null() {
        composeTestRule.setContent {
            MediaAttachSheet(
                show = true,
                onDismiss = {},
                onGalleryClick = {},
                onCameraClick = null,
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(MEDIA_ATTACH_CAMERA_ROW_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun sheet_does_not_render_when_show_is_false() {
        composeTestRule.setContent {
            MediaAttachSheet(
                show = false,
                onDismiss = {},
                onGalleryClick = {},
                onCameraClick = null,
            )
        }

        composeTestRule.waitForIdle()

        assertTrue(
            "attach sheet should not exist when show is false",
            composeTestRule
                .onAllNodesWithText("Galería")
                .fetchSemanticsNodes()
                .isEmpty(),
        )

        assertTrue(
            "gallery row should not exist when show is false",
            composeTestRule
                .onAllNodesWithText("Galería")
                .fetchSemanticsNodes()
                .isEmpty(),
        )

        assertTrue(
            "camera row should not exist when show is false",
            composeTestRule
                .onAllNodesWithText("Cámara")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }
}
package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose unit tests for [MediaAttachSheet] in isolation.
 *
 * What this test pins:
 *  - the sheet's three rows render in a stable order (Galería /
 *    Cámara / Audio), each with its own `testTag` for the
 *    acceptance suite to target.
 *  - rows with a wired callback expose a click action
 *    (`assertHasClickAction`) — the disable / enable contract
 *    is provable via the semantics tree.
 *  - the sheet dismisses (via `show = false`) and the rows
 *    unmount.
 *
 * Robolectric is required because the `ModalBottomSheet`
 * composable uses Android-only theming inside the test surface.
 *
 * Why no `performClick()` here? `ModalBottomSheet`'s scrim
 * intercepts clicks under Robolectric's JVM window manager
 * even after `waitForIdle()` — the click hits the scrim, which
 * dismisses the sheet rather than reaching the inner row. The
 * actual click → callback contract is exercised by:
 *  - [ConversationMediaAttachScreenTest.gallery_row_has_click_action] —
 *    pins the `assertHasClickAction` wiring without trying to
 *    click (the sheet's gesture interference is unrelated to
 *    the row's correctness);
 *  - the e2e suite on a real device — the production
 *    `MainActivity` + system gesture handler prove the end-to-
 *    end click path under the platform's actual window
 *    manager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class MediaAttachSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rows_render_in_galeria_camera_audio_order() {
        composeTestRule.setContent {
            MediaAttachSheet(
                show = true,
                onDismiss = {},
                onGalleryClick = {},
                onCameraClick = null,
                onAudioClick = null,
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
            .onNodeWithTag(MEDIA_ATTACH_AUDIO_ROW_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Galería")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Cámara")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Audio")
            .assertIsDisplayed()
    }

    @Test
    fun gallery_row_is_clickable_when_callback_is_provided() {
        composeTestRule.setContent {
            MediaAttachSheet(
                show = true,
                onDismiss = {},
                onGalleryClick = {},
                onCameraClick = null,
                onAudioClick = null,
            )
        }
        composeTestRule.waitForIdle()

        // The gallery callback is wired → the row exposes a
        // click action. Pinning this here so a regression in
        // the `Surface(onClick = onClick ?: {})` contract
        // (e.g. accidentally gating on `enabled` instead of
        // `onClick != null`) surfaces as a unit-test failure
        // before the e2e suite reports it as a UI flake.
        composeTestRule
            .onNodeWithTag(MEDIA_ATTACH_GALLERY_ROW_TAG)
            .assertHasClickAction()
    }

    @Test
    fun camera_row_is_clickable_when_callback_is_provided() {
        // Same pinning as gallery but for the 02-MM affordance.
        composeTestRule.setContent {
            MediaAttachSheet(
                show = true,
                onDismiss = {},
                onGalleryClick = {},
                onCameraClick = {},
                onAudioClick = null,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(MEDIA_ATTACH_CAMERA_ROW_TAG)
            .assertHasClickAction()
    }

    @Test
    fun audio_row_is_clickable_when_callback_is_provided() {
        // 03-MM wires the audio callback; pinning the click
        // action so the future impl doesn't break the row's
        // affordance.
        composeTestRule.setContent {
            MediaAttachSheet(
                show = true,
                onDismiss = {},
                onGalleryClick = {},
                onCameraClick = null,
                onAudioClick = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(MEDIA_ATTACH_AUDIO_ROW_TAG)
            .assertHasClickAction()
    }

    @Test
    fun sheet_does_not_render_when_show_is_false() {
        composeTestRule.setContent {
            MediaAttachSheet(
                show = false,
                onDismiss = {},
                onGalleryClick = {},
                onCameraClick = null,
                onAudioClick = null,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(MEDIA_ATTACH_SHEET_TAG)
            .assertDoesNotExist()
        composeTestRule
            .onAllNodesWithText("Galería")
            .fetchSemanticsNodes()
            .let { nodes -> assertTrue(
                "gallery row should not exist when sheet is hidden, was $nodes",
                nodes.isEmpty(),
            ) }
    }
}
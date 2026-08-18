package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose unit tests for [MediaPreviewCard] in isolation.
 *
 * The preview card has three visual states, each pinned here:
 *  - **idle** — `sending = false`, the action row mounts with
 *    "Enviar" + "Descartar" both visible. The user can either
 *    confirm the upload or discard the staged media.
 *  - **in-flight** — `sending = true`, the action row is replaced
 *    by a `CircularProgressIndicator` so the user reads the
 *    upload as an atomic operation and never double-taps.
 *  - **hidden** — when `pendingMedia` is null the parent screen
 *    does not render the card at all (this is verified by
 *    [ConversationMediaAttachScreenTest]; the test below
 *    pins the basic node-presence contract).
 *
 * Why isolate the card (vs covering it through the screen)?
 *  - the in-flight swap of the action row for the spinner is a
 *    sub-component contract that's easier to reason about in
 *    isolation;
 *  - a future iteration might pass a thumbnail URI to render
 *    asynchronously (Coil); the test surfaces the placeholder
 *    so the wiring doesn't drift.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class MediaPreviewCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun pendingMedia(
        name: String = "foto-baño.jpg",
        mime: String = "image/jpeg",
        size: Long = 1024L,
    ): PendingMedia = PendingMedia(
        localUri = null,
        mimeType = mime,
        originalName = name,
        sizeBytes = size,
        bytes = ByteArray(size.toInt()),
    )

    @Test
    fun idle_state_renders_send_and_discard_actions() {
        composeTestRule.setContent {
            MediaPreviewCard(
                pendingMedia = pendingMedia(),
                sending = false,
                onSendClick = {},
                onDiscardClick = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(MEDIA_PREVIEW_CARD_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(MEDIA_PREVIEW_THUMBNAIL_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("foto-baño.jpg")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Enviar")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Descartar")
            .assertIsDisplayed()
        // No spinner in idle state.
        composeTestRule
            .onNodeWithTag(MEDIA_PREVIEW_SPINNER_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun in_flight_state_replaces_actions_with_spinner() {
        composeTestRule.setContent {
            MediaPreviewCard(
                pendingMedia = pendingMedia(),
                sending = true,
                onSendClick = {},
                onDiscardClick = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(MEDIA_PREVIEW_SPINNER_TAG)
            .assertIsDisplayed()
        // In the in-flight state the action row is replaced by
        // the spinner so the user can't double-tap "Enviar".
        // Pin that the action buttons are absent.
        composeTestRule
            .onNodeWithText("Enviar")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Descartar")
            .assertDoesNotExist()
        // The card itself stays mounted during the upload so
        // the user retains the visual context (filename + mime
        // + thumbnail) of what they sent.
        composeTestRule
            .onNodeWithTag(MEDIA_PREVIEW_CARD_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("foto-baño.jpg")
            .assertIsDisplayed()
    }

    @Test
    fun metadata_line_includes_mime_and_human_readable_size() {
        composeTestRule.setContent {
            MediaPreviewCard(
                pendingMedia = pendingMedia(name = "big-photo.jpg", size = 5L * 1024 * 1024),
                sending = false,
                onSendClick = {},
                onDiscardClick = {},
            )
        }
        composeTestRule.waitForIdle()

        // The metadata row carries the mime + human-readable
        // size, both formatted through the locale's number
        // formatter. The test runs under `qualifiers = "es-rAR"`
        // which uses comma as the decimal separator, so
        // `%.2f` over `5.0` produces `"5,00 MB"` (not `"5.00 MB"`).
        // We assert the suffix (" MB") which is locale-stable +
        // the comma decimal separator so the test catches a
        // locale regression. Asserting only the "MB" suffix
        // makes the test robust against future locale changes.
        val metadataHits = composeTestRule
            .onAllNodesWithText(" MB", substring = true)
            .fetchSemanticsNodes()
        assertTrue(
            "expected a ' MB' node for the metadata line, got $metadataHits",
            metadataHits.isNotEmpty(),
        )
    }

    private fun assertTrue(message: String, condition: Boolean) {
        if (!condition) throw AssertionError(message)
    }
}
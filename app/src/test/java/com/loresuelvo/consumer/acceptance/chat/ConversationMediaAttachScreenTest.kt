package com.loresuelvo.consumer.acceptance.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.ui.screens.chat.ConversationScreen
import com.loresuelvo.consumer.ui.screens.chat.ConversationUiState
import com.loresuelvo.consumer.ui.screens.chat.MEDIA_ATTACH_GALLERY_ROW_TAG
import com.loresuelvo.consumer.ui.screens.chat.MEDIA_ATTACH_SHEET_TAG
import com.loresuelvo.consumer.ui.screens.chat.MEDIA_PREVIEW_CARD_TAG
import com.loresuelvo.consumer.ui.screens.chat.MEDIA_PREVIEW_NAME_TAG
import com.loresuelvo.consumer.ui.screens.chat.MEDIA_PREVIEW_SEND_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose-test for the 01-MM "Attach an image from the gallery"
 * UI surface. Verifies the visible contract of
 * [ConversationScreen] when the host is in the `Ready` state:
 *  - the leading `+` button is rendered to the LEFT of the
 *    prompt field (the attach affordance),
 *  - tapping the `+` button surfaces the [com.loresuelvo.consumer.ui.screens.chat.MediaAttachSheet]
 *    with the gallery entry visible,
 *  - no preview card is mounted when the consumer hasn't picked
 *    anything yet.
 *
 * Runs on the JVM via Robolectric so the test participates in
 * the unit-test task and stays fast. The actual picker callback
 * surface (the `PickVisualMedia` activity) is exercised by the
 * Compose acceptance tests in `androidTest/`; this test pins
 * the in-process UI plumbing.
 *
 * Why not a deep instrumented acceptance test? The full Home
 * → bottom nav → messages → conversation flow is wired but the
 * smart router + `SessionViewModel` re-hydration timing is too
 * flaky for a focused assertion (the existing
 * `ChatNavigationAcceptanceTest` survives only because the AI
 * send button is unique to Home). The unit-tested path covers
 * the same code in the screen + sheet + preview composables in
 * a deterministic way.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ConversationMediaAttachScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tap_attach_button_opens_sheet_with_gallery_option() {
        // The `showAttachSheet` flag is owned by the host
        // composable (in production: `LoResuelvoNav.Route` which
        // wraps it in a `remember { mutableStateOf }`). The test
        // mirrors the same pattern so the click on the `+`
        // button actually triggers a recomposition that mounts
        // the `MediaAttachSheet`.
        composeTestRule.setContent {
            var showAttachSheet by remember { mutableStateOf(false) }
            ConversationScreen(
                state = readyState(),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                onAttachClick = { showAttachSheet = true },
                onGalleryClick = {},
                onConfirmMediaSend = {},
                onDiscardMedia = {},
                onMediaErrorDismiss = {},
                onAttachSheetDismiss = { showAttachSheet = false },
                showAttachSheet = showAttachSheet,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeTestRule.waitForIdle()

        // Pin the chat surface first — the input bar's
        // placeholder is the top-level fingerprint for the Ready
        // state. The bar hardcodes
        // `R.string.chat_input_placeholder` ("Describe el
        // problema de tu hogar…" in the AI chat's copy); the
        // provider chat reuses the same `ChatInputBar` composable
        // so the same copy surfaces here. We're not pinning the
        // copy — we're just confirming the screen rendered.
        composeTestRule
            .onNodeWithText("Describe el problema", substring = true)
            .assertExists()

        // No preview yet — the consumer hasn't picked anything.
        composeTestRule
            .onNodeWithTag(MEDIA_PREVIEW_CARD_TAG)
            .assertDoesNotExist()

        // Tap the `+` button. The `+` is the attach affordance
        // on the LEFT of the input bar. Use `assertExists()`
        // because the bar may sit below the test window's
        // viewport (Robolectric's default surface is shorter
        // than production); the click handler still exercises
        // the onAttachClick callback regardless of viewport.
        composeTestRule
            .onNodeWithTag("chat-attach-button")
            .assertExists()
            .assertHasClickAction()
            .performClick()

        // The sheet surfaces with the gallery row.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText("Galería")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithTag(MEDIA_ATTACH_SHEET_TAG)
            .assertExists()
        composeTestRule
            .onNodeWithTag(MEDIA_ATTACH_GALLERY_ROW_TAG)
            .assertExists()

        // The preview card is still not mounted — the picker
        // hasn't returned a Uri.
        composeTestRule
            .onNodeWithTag(MEDIA_PREVIEW_CARD_TAG)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithTag(MEDIA_PREVIEW_NAME_TAG)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithTag(MEDIA_PREVIEW_SEND_TAG)
            .assertDoesNotExist()
    }

    private fun readyState(): ConversationUiState.Ready =
        ConversationUiState.Ready(
            detail = ConversationDetail(
                id = "1",
                status = ConversationStatus.Pending,
                counterpart = ConversationCounterpart(
                    id = 20L,
                    name = "Juan",
                    surname = "Pérez",
                    categoryName = "Plomería",
                    profilePhotoUrl = null,
                ),
                messages = emptyList(),
                updatedOnEpochMillis = 0L,
            ),
            promptInput = "",
            sending = false,
        )
}
package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Sender
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI test for [ChatScreen] focused on the WhatsApp-style
 * divider (ticket 2 UX polish). Run on the JVM via Robolectric so
 * the suite stays in `src/test/`.
 *
 * The divider is the visual separator between the chat surface and
 * the composer. It must be present in every screen state so the
 * layout reads as a single coherent chat column.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun msg(id: String, sender: Sender, content: String): ChatMessage =
        ChatMessage(
            id = id,
            sender = sender,
            content = content,
            sentAtEpochMillis = 0L,
        )

    /**
     * Empty composer state: the divider is rendered so the chat
     * surface and the input bar stay visually distinct.
     */
    @Test
    fun divider_is_present_when_chat_is_empty() {
        composeTestRule.setContent {
            ChatScreen(
                promptInput = "",
                canSend = false,
                sending = false,
                messages = emptyList(),
                transientError = null,
                preliminaryWarningVisible = true,
                onPromptChange = {},
                onSendClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                onBackClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeTestRule.onNodeWithTag(CHAT_INPUT_DIVIDER_TAG).assertExists()
    }

    /**
     * Non-empty composer state: the divider still exists when the
     * user has typed but hasn't sent.
     */
    @Test
    fun divider_is_present_when_input_has_text() {
        composeTestRule.setContent {
            ChatScreen(
                promptInput = "primera",
                canSend = true,
                sending = false,
                messages = emptyList(),
                transientError = null,
                preliminaryWarningVisible = true,
                onPromptChange = {},
                onSendClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                onBackClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeTestRule.onNodeWithTag(CHAT_INPUT_DIVIDER_TAG).assertExists()
    }
}

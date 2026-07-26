package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Sender
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI test for [ChatScreen] focused on the chat surface and
 * the divider. Run on the JVM via Robolectric.
 *
 * Commit 11G-final: the initial message is always the first item in
 * the conversation (whether the user has sent messages or not),
 * so the "welcome" feeling comes from the bubble alone, not from a
 * separate hero composable.
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

    /**
     * Empty messages list: the initial assistant message bubble is
     * still rendered (it's always the first item in the
     * conversation). The "welcome" feel comes from this bubble
     * alone, not from a separate hero composable.
     */
    @Test
    fun initial_message_bubble_is_visible_when_messages_is_empty() {
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
        composeTestRule
            .onNodeWithText(
                androidx.test.core.app.ApplicationProvider
                    .getApplicationContext<android.content.Context>()
                    .getString(R.string.chat_initial_message_body),
            )
            .assertExists()
    }

    /**
     * Non-empty messages list: the initial message is the first
     * item in the list, followed by the user's and the assistant's
     * subsequent messages. Test verifies the initial bubble
     * remains present (it's the "first message" of the
     * conversation).
     */
    @Test
    fun initial_message_bubble_remains_when_messages_is_not_empty() {
        composeTestRule.setContent {
            ChatScreen(
                promptInput = "",
                canSend = false,
                sending = false,
                messages = listOf(
                    msg("user-1", Sender.Consumer, "primera"),
                    msg(
                        "assistant-1",
                        Sender.Assistant,
                        "contame qué está ocurriendo",
                    ),
                ),
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
        composeTestRule
            .onNodeWithText(
                androidx.test.core.app.ApplicationProvider
                    .getApplicationContext<android.content.Context>()
                    .getString(R.string.chat_initial_message_body),
            )
            .assertExists()
    }

    /**
     * Every assistant message — initial bubble included — renders
     * the [ASSISTANT_AVATAR_TAG] next to the bubble text. The
     * count is `1 (initial) + count(Sender.Assistant in messages)`.
     */
    @Test
    fun initial_bubble_renders_avatar() {
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
        // Only the initial bubble exists; it has its avatar.
        composeTestRule
            .onAllNodesWithTag(ASSISTANT_AVATAR_TAG, useUnmergedTree = true)
            .assertCountEquals(1)
    }

    /**
     * A user-sent assistant message also renders the avatar (not
     * just the initial bubble). Conversation = [initial, user-1,
     * assistant-1] = 2 assistant messages = 2 avatars.
     */
    @Test
    fun user_sent_assistant_message_renders_avatar() {
        composeTestRule.setContent {
            ChatScreen(
                promptInput = "",
                canSend = false,
                sending = false,
                messages = listOf(
                    msg("user-1", Sender.Consumer, "primera"),
                    msg(
                        "assistant-1",
                        Sender.Assistant,
                        "contame qué está ocurriendo",
                    ),
                ),
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
        composeTestRule
            .onAllNodesWithTag(ASSISTANT_AVATAR_TAG, useUnmergedTree = true)
            .assertCountEquals(2)
    }
}

package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.ui.screens.chat.components.CONVERSATION_MESSAGE_BUBBLE_TAG
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.ui.screens.chat.components.CONVERSATION_MESSAGE_AUDIO_PLAY_TAG
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Companion to `ConversationScreenTest.kt` — covers the messages
 * list, the composer, and the transient send-error card. The
 * split keeps each test file within the per-commit LoC budget.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ConversationScreenMessagesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun localizedString(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(resourceId)

    private fun counterpart() = ConversationCounterpart(
        id = 20L,
        name = "Juan",
        surname = "Gómez",
        categoryName = "Plomería",
        profilePhotoUrl = null,
    )

    private fun message(
        id: String,
        content: String,
        sender: ConversationSender = ConversationSender.Consumer,
    ) = ConversationMessage(
        id = id,
        sender = sender,
        content = content,
        createdOnEpochMillis = 1_700_000_000_000L,
    )

    private fun audioMessage(
        id: String = "audio-msg-1",
        sender: ConversationSender = ConversationSender.Consumer,
    ) = ConversationMessage(
        id = id,
        sender = sender,
        content = "",
        createdOnEpochMillis = 1_700_000_000_000L,
        media = MediaReference.Audio(
            id = "audio-file-id",
            url = "https://cdn.loresuelvo.test/nota-10s.webm",
            mimeType = "audio/webm",
            originalName = "nota-10s.webm",
            durationMillis = 10_000L,
        ),
    )

    private fun detail(
        messages: List<ConversationMessage> = emptyList(),
    ) = ConversationDetail(
        id = "1",
        status = ConversationStatus.Pending,
        counterpart = counterpart(),
        messages = messages,
        updatedOnEpochMillis = 0L,
    )

    private fun readyState(
        promptInput: String = "",
        sending: Boolean = false,
        transientError: SendMessageOutcome.Failure? = null,
        detail: ConversationDetail = detail(),
    ) = ConversationUiState.Ready(
        detail = detail,
        promptInput = promptInput,
        sending = sending,
        transientError = transientError,
    )

    // ---- Messages list ---------------------------------------------------

    @Test
    fun ready_state_renders_one_bubble_per_message() {
        val messages = listOf(
            message(id = "1", content = "Hola", sender = ConversationSender.Consumer),
            message(id = "2", content = "¡Hola! ¿Cuándo podés?", sender = ConversationSender.Provider),
            message(id = "3", content = "Mañana a las 10", sender = ConversationSender.Consumer),
        )

        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(detail = detail(messages = messages)),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithTag(CONVERSATION_LIST_TAG).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_MESSAGE_BUBBLE_TAG)
            .assertCountEquals(3)
        composeTestRule.onNodeWithText("Hola").assertIsDisplayed()
        composeTestRule.onNodeWithText("¡Hola! ¿Cuándo podés?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mañana a las 10").assertIsDisplayed()
    }

    @Test
    fun ready_state_with_empty_messages_renders_no_bubbles() {
        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(detail = detail(messages = emptyList())),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithTag(CONVERSATION_LIST_TAG).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_MESSAGE_BUBBLE_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun ready_state_audio_play_button_fires_onPlayAudio_with_message_id() {
        var playedMessageId: String? = null

        val audioMessage = audioMessage(id = "audio-msg-1")

        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(
                    detail = detail(
                        messages = listOf(audioMessage),
                    ),
                ),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                onPlayAudio = { messageId ->
                    playedMessageId = messageId
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_AUDIO_PLAY_TAG)
            .assertIsDisplayed()
            .performClick()

        assertTrue(playedMessageId == "audio-msg-1")
    }

    // ---- Composer --------------------------------------------------------

    @Test
    fun ready_state_composer_renders_prompt_text() {
        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(promptInput = "Hola Juan, ¿mañana podés?"),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithText("Hola Juan, ¿mañana podés?")
            .assertIsDisplayed()
    }

    @Test
    fun ready_state_send_button_fires_onSendClick() {
        var sendClicks = 0

        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(promptInput = "hola"),
                onPromptChange = {},
                onSendClick = { sendClicks++ },
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithTag(com.loresuelvo.consumer.ui.screens.chat.SEND_BUTTON_TAG)
            .assertIsDisplayed()
            .performClick()
        assertTrue(sendClicks == 1)
    }

    @Test
    fun ready_state_prompt_change_fires_callback() {
        var lastValue = ""

        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(promptInput = ""),
                onPromptChange = { lastValue = it },
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithTag(com.loresuelvo.consumer.ui.screens.chat.CHAT_INPUT_FIELD_TAG)
            .performTextInput("editando")

        assertTrue(lastValue == "editando")
    }

    // ---- Transient error card --------------------------------------------

    @Test
    fun ready_state_renders_transient_error_card_when_failure_is_set() {
        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(
                    transientError = SendMessageOutcome.Failure.Server(500, "boom"),
                ),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_TRANSIENT_ERROR_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(localizedString(R.string.conversation_transient_error_server))
            .assertIsDisplayed()
    }

    @Test
    fun ready_state_hides_transient_error_card_when_failure_is_null() {
        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(transientError = null),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_TRANSIENT_ERROR_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun ready_state_transient_error_retry_and_dismiss_fire_callbacks() {
        var retryClicks = 0
        var dismissClicks = 0

        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(
                    transientError = SendMessageOutcome.Failure.Server(500, "boom"),
                ),
                onPromptChange = {},
                onSendClick = { retryClicks++ },
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = { dismissClicks++ },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_TRANSIENT_ERROR_RETRY_TAG)
            .assertIsDisplayed()
            .performClick()
        assertTrue(retryClicks == 1)

        composeTestRule
            .onNodeWithTag(CONVERSATION_TRANSIENT_ERROR_DISMISS_TAG)
            .assertIsDisplayed()
            .performClick()
        assertTrue(dismissClicks == 1)
    }
}
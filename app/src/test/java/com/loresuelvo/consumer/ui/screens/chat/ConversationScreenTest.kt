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
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.ui.screens.chat.components.CONVERSATION_MESSAGE_BUBBLE_TAG
import com.loresuelvo.consumer.ui.screens.chat.components.CONVERSATION_TOP_BAR_BACK_TAG
import com.loresuelvo.consumer.ui.screens.chat.components.CONVERSATION_TOP_BAR_PENDING_TAG
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI test for [ConversationScreen] — Loading / Error /
 * top bar surface. Companion file
 * `ConversationScreenMessagesTest.kt` covers the messages list,
 * composer, and transient error card (split to keep each file
 * within the per-commit LoC budget).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ConversationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun localizedString(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(resourceId)

    private fun counterpart(
        name: String = "Juan",
        surname: String = "Gómez",
        categoryName: String = "Plomería",
    ) = ConversationCounterpart(
        id = 20L,
        name = name,
        surname = surname,
        categoryName = categoryName,
        profilePhotoUrl = null,
    )

    private fun detail(
        id: String = "1",
        status: ConversationStatus = ConversationStatus.Pending,
        messages: List<ConversationMessage> = emptyList(),
        counterpart: ConversationCounterpart = counterpart(),
    ) = ConversationDetail(
        id = id,
        status = status,
        counterpart = counterpart,
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

    // ---- Loading ---------------------------------------------------------

    @Test
    fun loading_state_renders_spinner() {
        composeTestRule.setContent {
            ConversationScreen(
                state = ConversationUiState.Loading,
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithTag(CONVERSATION_LOADING_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(localizedString(R.string.conversation_loading))
            .assertIsDisplayed()
    }

    // ---- Error -----------------------------------------------------------

    @Test
    fun error_state_Network_renders_network_copy_and_retry_button() {
        var retryClicks = 0

        composeTestRule.setContent {
            ConversationScreen(
                state = ConversationUiState.Error(
                    failure = ConversationDetailOutcome.Failure.Network(
                        cause = java.io.IOException("dns"),
                    ),
                ),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = { retryClicks++ },
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithTag(CONVERSATION_ERROR_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(localizedString(R.string.conversation_error_network))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CONVERSATION_ERROR_RETRY_TAG)
            .assertIsDisplayed()
            .performClick()
        assertTrue(retryClicks == 1)
    }

    @Test
    fun error_state_Server_renders_server_copy() {
        composeTestRule.setContent {
            ConversationScreen(
                state = ConversationUiState.Error(
                    failure = ConversationDetailOutcome.Failure.Server(500, "boom"),
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
            .onNodeWithText(localizedString(R.string.conversation_error_server))
            .assertIsDisplayed()
    }

    @Test
    fun error_state_Unauthorized_renders_unauthorized_copy() {
        composeTestRule.setContent {
            ConversationScreen(
                state = ConversationUiState.Error(
                    failure = ConversationDetailOutcome.Failure.Unauthorized(
                        message = "token expired",
                    ),
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
            .onNodeWithText(localizedString(R.string.conversation_error_unauthorized))
            .assertIsDisplayed()
    }

    // ---- Ready: top bar --------------------------------------------------

    @Test
    fun ready_state_renders_top_bar_with_counterpart_full_name_and_category() {
        var backClicks = 0

        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(
                    detail = detail(
                        counterpart = counterpart(
                            name = "Lucía",
                            surname = "Pérez",
                            categoryName = "Gas",
                        ),
                    ),
                ),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = { backClicks++ },
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithText("Lucía Pérez")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Gas").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CONVERSATION_TOP_BAR_BACK_TAG)
            .assertIsDisplayed()
            .performClick()
        assertTrue(backClicks == 1)
    }

    @Test
    fun ready_state_renders_pending_pill_when_status_is_Pending() {
        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(detail = detail(status = ConversationStatus.Pending)),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_TOP_BAR_PENDING_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(localizedString(R.string.messages_screen_pending_badge))
            .assertIsDisplayed()
    }

    @Test
    fun ready_state_hides_pending_pill_when_status_is_Other() {
        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(detail = detail(status = ConversationStatus.Other("accepted"))),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_TOP_BAR_PENDING_TAG)
            .assertCountEquals(0)
    }
}
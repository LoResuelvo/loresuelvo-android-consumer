package com.loresuelvo.consumer.ui.screens.messages

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
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.ui.screens.messages.components.CONVERSATION_ROW_PENDING_TAG
import com.loresuelvo.consumer.ui.screens.messages.components.CONVERSATION_ROW_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI test for [MessagesScreen]. Run on the JVM via
 * Robolectric. Verifies each state branch:
 *
 *  - [MessagesListUiState.Loading] shows the spinner slot.
 *  - [MessagesListUiState.Ready] with empty list renders the
 *    empty-state card with localized copy.
 *  - [MessagesListUiState.Ready] with a non-empty list renders
 *    one [CONVERSATION_ROW_TAG] per conversation (keyed by
 *    [Conversation.id] so reordering / refreshes don't lose
 *    scroll position).
 *  - Pending conversations render the
 *    [CONVERSATION_ROW_PENDING_TAG] badge; non-pending ones
 *    don't.
 *  - Conversations whose [Conversation.lastMessage] is `null`
 *    render the "no messages yet" fallback string.
 *  - [MessagesListUiState.Error] for each failure subtype
 *    (Network / Server / Unauthorized) renders the corresponding
 *    localized copy and the retry button.
 *
 * The CI defaults to `es-rAR` (see
 * `app/build.gradle.kts` test JVM args `-Duser.country=AR
 * -Duser.language=es`), so the localized copy resolves from
 * `values/strings.xml`. The English matrix is covered by the
 * `make e2e` acceptance tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class MessagesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun localizedString(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(resourceId)

    private fun conversation(
        id: String,
        providerName: String = "Juan",
        providerSurname: String = "Gómez",
        status: ConversationStatus = ConversationStatus.Pending,
        lastMessageContent: String? = "Hola Juan, necesito una mano",
        lastMessageSender: ConversationSender = ConversationSender.Consumer,
        updatedOn: Long = System.currentTimeMillis(),
    ) = Conversation(
        id = id,
        status = status,
        counterpart = ConversationCounterpart(
            id = 20L,
            name = providerName,
            surname = providerSurname,
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        lastMessage = lastMessageContent?.let { content ->
            ConversationMessage(
                id = "$id-msg-1",
                sender = lastMessageSender,
                content = content,
                createdOnEpochMillis = updatedOn,
            )
        },
        updatedOnEpochMillis = updatedOn,
    )

    // ---- Loading state --------------------------------------------------

    @Test
    fun loading_state_renders_spinner() {
        composeTestRule.setContent {
            MessagesScreen(
                state = MessagesListUiState.Loading,
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithTag(MESSAGES_LOADING_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(localizedString(R.string.messages_screen_loading))
            .assertIsDisplayed()
    }

    // ---- Empty state ----------------------------------------------------

    @Test
    fun ready_state_with_empty_list_renders_empty_card() {
        composeTestRule.setContent {
            MessagesScreen(
                state = MessagesListUiState.Ready(conversations = emptyList()),
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithTag(MESSAGES_EMPTY_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(localizedString(R.string.messages_screen_empty_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(localizedString(R.string.messages_screen_empty_body))
            .assertIsDisplayed()
        // The list container must NOT exist when the list is empty.
        composeTestRule.onNodeWithTag(MESSAGES_LIST_TAG).assertDoesNotExist()
    }

    // ---- Ready with a non-empty list ------------------------------------

    @Test
    fun ready_state_with_list_renders_one_row_per_conversation() {
        val conversations = listOf(
            conversation(id = "1", providerName = "Juan", providerSurname = "Gómez"),
            conversation(id = "2", providerName = "Pedro", providerSurname = "Dib"),
            conversation(id = "3", providerName = "Lucía", providerSurname = "Pérez"),
        )

        composeTestRule.setContent {
            MessagesScreen(
                state = MessagesListUiState.Ready(conversations = conversations),
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithTag(MESSAGES_LIST_TAG).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ROW_TAG, useUnmergedTree = true)
            .assertCountEquals(3)
        // The counterpart full name lands in the row.
        composeTestRule
            .onNodeWithText("Juan Gómez")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Pedro Dib")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Lucía Pérez")
            .assertIsDisplayed()
    }

    @Test
    fun ready_state_renders_pending_badge_only_for_pending_conversations() {
        val conversations = listOf(
            // Pending → badge renders.
            conversation(
                id = "1",
                providerName = "Juan",
                providerSurname = "Gómez",
                status = ConversationStatus.Pending,
            ),
            // Other("accepted") → no badge.
            conversation(
                id = "2",
                providerName = "Pedro",
                providerSurname = "Dib",
                status = ConversationStatus.Other(raw = "accepted"),
            ),
        )

        composeTestRule.setContent {
            MessagesScreen(
                state = MessagesListUiState.Ready(conversations = conversations),
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Exactly one badge (the Pending row). The badge is a
        // child of a clickable Row, so its testTag is consumed
        // by the merged semantics tree unless we opt into the
        // unmerged view (same pattern as `ChatScreenTest`).
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ROW_PENDING_TAG, useUnmergedTree = true)
            .assertCountEquals(1)
        // The badge carries the localized copy.
        composeTestRule
            .onNodeWithText(localizedString(R.string.messages_screen_pending_badge))
            .assertIsDisplayed()
    }

    @Test
    fun ready_state_renders_no_preview_string_when_lastMessage_is_null() {
        val conversations = listOf(
            conversation(
                id = "1",
                providerName = "Juan",
                providerSurname = "Gómez",
                lastMessageContent = null,
            ),
        )

        composeTestRule.setContent {
            MessagesScreen(
                state = MessagesListUiState.Ready(conversations = conversations),
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithText(localizedString(R.string.messages_screen_no_preview))
            .assertIsDisplayed()
    }

    @Test
    fun tapping_a_row_fires_onConversationClick_with_the_conversation_id() {
        // Regression test: commit 7a wired `onClick` as a parameter
        // on `ConversationRow` but the parameter was never wired
        // into a Modifier.clickable, so the row was inert. This
        // test would have caught it.
        val conversations = listOf(
            conversation(id = "1", providerName = "Juan", providerSurname = "Gómez"),
            conversation(id = "2", providerName = "Pedro", providerSurname = "Dib"),
        )
        var clickedId: String? = null

        composeTestRule.setContent {
            MessagesScreen(
                state = MessagesListUiState.Ready(conversations = conversations),
                onConversationClick = { clickedId = it },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ROW_TAG)
            .get(1)
            .performClick()

        assertEquals("2", clickedId)
    }

    // ---- Error state ----------------------------------------------------

    @Test
    fun error_state_Network_renders_network_copy_and_retry_button() {
        var retryClicks = 0

        composeTestRule.setContent {
            MessagesScreen(
                state = MessagesListUiState.Error(
                    failure = ConversationsOutcome.Failure.Network(cause = java.io.IOException("dns")),
                ),
                onRetryClick = { retryClicks++ },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithTag(MESSAGES_ERROR_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(localizedString(R.string.messages_screen_error_network))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(MESSAGES_ERROR_RETRY_TAG)
            .assertIsDisplayed()
            .performClick()
        assertTrue("retry must fire onRetryClick", retryClicks == 1)
    }

    @Test
    fun error_state_Server_renders_server_copy() {
        composeTestRule.setContent {
            MessagesScreen(
                state = MessagesListUiState.Error(
                    failure = ConversationsOutcome.Failure.Server(code = 500, message = "boom"),
                ),
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithText(localizedString(R.string.messages_screen_error_server))
            .assertIsDisplayed()
    }

    @Test
    fun error_state_Unauthorized_renders_unauthorized_copy() {
        composeTestRule.setContent {
            MessagesScreen(
                state = MessagesListUiState.Error(
                    failure = ConversationsOutcome.Failure.Unauthorized(message = "token expired"),
                ),
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithText(localizedString(R.string.messages_screen_error_unauthorized))
            .assertIsDisplayed()
    }
}
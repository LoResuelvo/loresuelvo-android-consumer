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
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.ui.screens.chat.components.NEW_MESSAGE_BANNER_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI coverage for the real-time chat wire (scenarios
 * 09-IC / 10-IC): the "↓ nuevo mensaje" banner surfaces when
 * the consumer is scrolled up and a provider message arrives,
 * and tapping it scrolls the list back to the bottom.
 *
 * The state-level coverage of the same flow lives in
 * [ConversationViewModelTest]'s scroll-tracking section. These
 * tests pin the *visual* surface that the unit tests can't see:
 * banner render, banner tap → scroll-to-bottom.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ConversationScreenScrollTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun localizedString(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(resourceId)

    private fun detail(
        id: String = "1",
        messages: List<ConversationMessage> = (1..15).map { i ->
            ConversationMessage(
                id = "$i",
                sender = if (i % 2 == 0) ConversationSender.Consumer else ConversationSender.Provider,
                content = "Mensaje $i",
                createdOnEpochMillis = 1_700_000_000_000L + i * 1000L,
            )
        },
    ) = ConversationDetail(
        id = id,
        status = ConversationStatus.Pending,
        counterpart = ConversationCounterpart(
            id = 20L,
            name = "Juan",
            surname = "Gómez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        messages = messages,
        updatedOnEpochMillis = 0L,
    )

    private fun readyState(
        detail: ConversationDetail = detail(),
        hasUnreadIncoming: Boolean = false,
    ) = ConversationUiState.Ready(
        detail = detail,
        promptInput = "",
        sending = false,
        hasUnreadIncoming = hasUnreadIncoming,
    )

    @Test
    fun unread_banner_renders_when_hasUnreadIncoming_is_true() {
        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(hasUnreadIncoming = true),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithTag(NEW_MESSAGE_BANNER_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(localizedString(R.string.conversation_new_message_banner))
            .assertIsDisplayed()
    }

    @Test
    fun unread_banner_does_not_render_when_hasUnreadIncoming_is_false() {
        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(hasUnreadIncoming = false),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onAllNodesWithTag(NEW_MESSAGE_BANNER_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun unread_banner_tap_fires_onUnreadBannerTapped_callback() {
        // The visual scroll-to-bottom action is owned by the
        // screen (it has access to the LazyListState); the VM
        // callback just clears the unread flag. This test pins
        // that the screen routes the tap to the VM.
        var taps = 0

        composeTestRule.setContent {
            ConversationScreen(
                state = readyState(hasUnreadIncoming = true),
                onPromptChange = {},
                onSendClick = {},
                onBackClick = {},
                onRetryClick = {},
                onErrorDismiss = {},
                onUnreadBannerTapped = { taps++ },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule
            .onNodeWithTag(NEW_MESSAGE_BANNER_TAG)
            .performClick()

        assertEquals("banner tap must invoke the screen-level callback", 1, taps)
    }
}
package com.loresuelvo.consumer.ui.screens.chat.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.MediaReference
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ConversationMessageBubbleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun audioMessage(
        durationMillis: Long = 10_000L,
    ) = ConversationMessage(
        id = "audio-msg-1",
        sender = ConversationSender.Consumer,
        content = "",
        createdOnEpochMillis = 1_700_000_000_000L,
        media = MediaReference.Audio(
            id = "audio-file-id",
            url = "https://cdn.loresuelvo.test/nota-voz.webm",
            mimeType = "audio/webm",
            originalName = "nota-voz.webm",
            durationMillis = durationMillis,
        ),
    )

    private fun imageMessage() = ConversationMessage(
        id = "image-msg-1",
        sender = ConversationSender.Consumer,
        content = "",
        createdOnEpochMillis = 1_700_000_000_000L,
        media = MediaReference.Image(
            id = "image-file-id",
            url = "https://cdn.loresuelvo.test/gotera-baño.jpg",
            mimeType = "image/jpeg",
            originalName = "gotera-baño.jpg",
        ),
    )

    @Test
    fun image_message_renders_image() {
        composeTestRule.setContent {
            ConversationMessageBubble(
                message = imageMessage(),
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_BUBBLE_TAG)
            .fetchSemanticsNode()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_IMAGE_TAG)
            .fetchSemanticsNode()
    }

    @Test
    fun text_message_renders_content() {
        composeTestRule.setContent {
            ConversationMessageBubble(
                message = ConversationMessage(
                    id = "text-msg-1",
                    sender = ConversationSender.Consumer,
                    content = "Hola Juan",
                    createdOnEpochMillis = 1_700_000_000_000L,
                ),
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_BUBBLE_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Hola Juan")
            .assertIsDisplayed()
    }

    @Test
    fun audio_message_renders_audio_bubble() {
        composeTestRule.setContent {
            ConversationMessageBubble(
                message = audioMessage(),
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_BUBBLE_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_AUDIO_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun audio_message_renders_duration() {
        composeTestRule.setContent {
            ConversationMessageBubble(
                message = audioMessage(durationMillis = 10_000L),
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_AUDIO_DURATION_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("0:10")
            .assertIsDisplayed()
    }

    @Test
    fun audio_message_formats_minutes_and_seconds() {
        composeTestRule.setContent {
            ConversationMessageBubble(
                message = audioMessage(durationMillis = 65_000L),
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("1:05")
            .assertIsDisplayed()
    }

    @Test
    fun audio_message_renders_play_button() {
        composeTestRule.setContent {
            ConversationMessageBubble(
                message = audioMessage(durationMillis = 5_000L),
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_AUDIO_PLAY_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun audio_message_renders_progress_line() {
        composeTestRule.setContent {
            ConversationMessageBubble(
                message = audioMessage(durationMillis = 5_000L),
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_AUDIO_PROGRESS_TAG)
            .assertIsDisplayed()
    }
}
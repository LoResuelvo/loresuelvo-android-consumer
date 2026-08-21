package com.loresuelvo.consumer.ui.screens.chat.components

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performClick
import androidx.compose.foundation.clickable
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.ui.screens.chat.AudioPlaybackState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals

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

    @Test
    fun audio_message_renders_progress_for_current_position() {
        composeTestRule.setContent {
            ConversationMessageBubble(
                message = audioMessage(durationMillis = 10_000L),
                audioPlayback = AudioPlaybackState(
                    messageId = "audio-msg-1",
                    isPlaying = true,
                    currentPositionMillis = 2_000L,
                ),
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_AUDIO_PROGRESS_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun audio_message_shows_elapsed_time_while_playing() {
        composeTestRule.setContent {
            ConversationMessageBubble(
                message = audioMessage(durationMillis = 10_000L),
                audioPlayback = AudioPlaybackState(
                    messageId = "audio-msg-1",
                    isPlaying = true,
                    currentPositionMillis = 2_000L,
                ),
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("0:02")
            .assertIsDisplayed()
    }

    @Test
    fun audio_message_shows_zero_elapsed_time_when_not_playing() {
        composeTestRule.setContent {
            ConversationMessageBubble(
                message = audioMessage(durationMillis = 10_000L),
                audioPlayback = AudioPlaybackState(
                    messageId = null,
                    isPlaying = false,
                    currentPositionMillis = 0L,
                ),
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("0:00")
            .assertIsDisplayed()
    }

    @Test
    fun audio_message_play_button_invokes_callback() {
        var playedMessageId: String? = null

        composeTestRule.setContent {
            ConversationMessageBubble(
                message = audioMessage(durationMillis = 10_000L),
                onPlayAudio = { messageId ->
                    playedMessageId = messageId
                },
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_AUDIO_PLAY_TAG)
            .performClick()

        assertEquals(
            "audio-msg-1",
            playedMessageId,
        )
    }

    @Test
    fun audio_message_progress_updates_when_current_position_changes() {
        lateinit var playbackState: MutableState<AudioPlaybackState>

        composeTestRule.setContent {
            playbackState = remember {
                mutableStateOf(
                    AudioPlaybackState(
                        messageId = "audio-msg-1",
                        isPlaying = true,
                        currentPositionMillis = 0L,
                    ),
                )
            }

            ConversationMessageBubble(
                message = audioMessage(durationMillis = 10_000L),
                audioPlayback = playbackState.value,
            )
        }

        composeTestRule.waitForIdle()
        assertProgress(0f)

        playbackState.value = playbackState.value.copy(
            currentPositionMillis = 5_000L,
        )

        composeTestRule.waitForIdle()
        assertProgress(0.5f)
    }

    @Test
    fun audio_message_play_button_shows_pause_icon_when_playing() {
        composeTestRule.setContent {
            ConversationMessageBubble(
                message = audioMessage(durationMillis = 10_000L),
                audioPlayback = AudioPlaybackState(
                    messageId = "audio-msg-1",
                    isPlaying = true,
                    currentPositionMillis = 2_000L,
                ),
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_AUDIO_PLAY_TAG)
            .assertTextEquals(PAUSE_ICON)
    }

    @Test
    fun audio_message_play_button_shows_play_icon_when_not_playing() {
        composeTestRule.setContent {
            ConversationMessageBubble(
                message = audioMessage(durationMillis = 10_000L),
                audioPlayback = AudioPlaybackState(
                    messageId = null,
                    isPlaying = false,
                    currentPositionMillis = 0L,
                ),
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_AUDIO_PLAY_TAG)
            .assertTextEquals(PLAY_ICON)
    }

    @Test
    fun audio_message_play_button_fires_onPauseAudio_when_currently_playing() {
        var pausedMessageId: String? = null

        composeTestRule.setContent {
            ConversationMessageBubble(
                message = audioMessage(durationMillis = 10_000L),
                audioPlayback = AudioPlaybackState(
                    messageId = "audio-msg-1",
                    isPlaying = true,
                    currentPositionMillis = 2_000L,
                ),
                onPlayAudio = { /* unused */ },
                onPauseAudio = { messageId ->
                    pausedMessageId = messageId
                },
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_AUDIO_PLAY_TAG)
            .performClick()

        assertEquals(
            "audio-msg-1",
            pausedMessageId,
        )
    }

    private fun assertProgress(expected: Float) {
        val config = composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGE_AUDIO_PROGRESS_TAG)
            .fetchSemanticsNode()
            .config

        assert(
            config.contains(SemanticsProperties.ProgressBarRangeInfo),
        ) { "expected LinearProgressIndicator to expose ProgressBarRangeInfo" }

        val info = config[SemanticsProperties.ProgressBarRangeInfo]

        assertEquals(
            expected,
            info.current,
            0.001f,
        )
    }
}
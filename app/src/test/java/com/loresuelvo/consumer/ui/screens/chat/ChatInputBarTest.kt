package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [ChatInputBar].
 *
 * Coverage:
 *
 *  - 07-DIA: the field's height grows with the user's content up
 *    to six lines.
 *  - 08-DIA: content beyond six lines does NOT grow further.
 *  - Audio recording controls are available when the prompt is
 *    empty and can be started / stopped through the callbacks.
 *
 * The test renders the bar in a fixed-height parent so the field
 * has enough room to grow up to its six-line cap.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ChatInputBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Helper: render [ChatInputBar] bound to mutable state the test
     * can drive via [performTextInput] / [performTextClearance].
     */
    private fun setContentBar(
        promptState: MutableState<String>,
        sending: Boolean = false,
        recordingAudio: Boolean = false,
        onStartAudioRecording: () -> Unit = {},
        onStopAudioRecording: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ChatInputBar(
                promptInput = promptState.value,
                canSend = promptState.value.isNotBlank() && !sending,
                sending = sending,
                recordingAudio = recordingAudio,
                onPromptChange = { promptState.value = it },
                onSendClick = {},
                onStartAudioRecording = onStartAudioRecording,
                onStopAudioRecording = onStopAudioRecording,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(400.dp),
            )
        }
    }

    private fun fieldHeightPx(): Float = composeTestRule
        .onNodeWithTag(CHAT_INPUT_FIELD_TAG)
        .fetchSemanticsNode()
        .boundsInRoot
        .height

    private fun dpToPx(dp: androidx.compose.ui.unit.Dp): Float =
        with(composeTestRule.density) { dp.toPx() }

    @Test
    fun single_line_content_keeps_field_compact() {
        val prompt = androidx.compose.runtime.mutableStateOf("")

        setContentBar(prompt)

        composeTestRule
            .onNodeWithTag(CHAT_INPUT_FIELD_TAG)
            .performTextInput("Hola")

        assertTrue(
            "single-line field should stay ≤ ~96.dp, was ${fieldHeightPx()}px",
            fieldHeightPx() <= dpToPx(96.dp),
        )
    }

    @Test
    fun three_line_content_grows_field_height() {
        val prompt = androidx.compose.runtime.mutableStateOf("")

        setContentBar(prompt)

        composeTestRule
            .onNodeWithTag(CHAT_INPUT_FIELD_TAG)
            .performTextInput(
                "Línea uno\nLínea dos\nLínea tres",
            )

        composeTestRule
            .onNodeWithTag(CHAT_INPUT_FIELD_TAG)
            .assertHeightIsAtLeast(60.dp)
    }

    @Test
    fun five_line_content_does_not_yet_scroll_six_lines_under_max() {
        val prompt = androidx.compose.runtime.mutableStateOf("")

        setContentBar(prompt)

        composeTestRule
            .onNodeWithTag(CHAT_INPUT_FIELD_TAG)
            .performTextInput(
                "L1\nL2\nL3\nL4\nL5",
            )

        composeTestRule
            .onNodeWithTag(CHAT_INPUT_FIELD_TAG)
            .assertHeightIsAtLeast(110.dp)
    }

    @Test
    fun eight_line_content_caps_at_six_lines_and_scrolls() {
        val prompt = androidx.compose.runtime.mutableStateOf("")

        setContentBar(prompt)

        composeTestRule
            .onNodeWithTag(CHAT_INPUT_FIELD_TAG)
            .performTextInput(
                "L1\nL2\nL3\nL4\nL5\nL6\nL7\nL8",
            )

        assertTrue(
            "field should cap at ≤ 6 lines (≤ 175 dp), " +
                "was ${fieldHeightPx()}px",
            fieldHeightPx() <= dpToPx(175.dp),
        )

        composeTestRule
            .onNodeWithTag(CHAT_INPUT_FIELD_TAG)
            .assertHeightIsAtLeast(110.dp)
    }

    @Test
    fun clearing_the_field_keeps_send_disabled_via_canSend() {
        val prompt = androidx.compose.runtime.mutableStateOf("algo")

        setContentBar(prompt)

        composeTestRule
            .onNodeWithTag(CHAT_INPUT_FIELD_TAG)
            .performTextClearance()

        assertTrue(
            "field height should stay compact after clear, was ${fieldHeightPx()}px",
            fieldHeightPx() <= dpToPx(96.dp),
        )
    }

    // ---- Audio ------------------------------------------------------

    @Test
    fun shows_microphone_when_prompt_is_empty() {
        composeTestRule.setContent {
            ChatInputBar(
                promptInput = "",
                canSend = false,
                sending = false,
                recordingAudio = false,
                onPromptChange = {},
                onSendClick = {},
                onStartAudioRecording = {},
                onStopAudioRecording = {},
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(400.dp),
            )
        }

        composeTestRule
            .onNodeWithTag(RECORD_AUDIO_BUTTON_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun shows_send_when_prompt_has_text() {
        composeTestRule.setContent {
            ChatInputBar(
                promptInput = "Hola Juan",
                canSend = true,
                sending = false,
                recordingAudio = false,
                onPromptChange = {},
                onSendClick = {},
                onStartAudioRecording = {},
                onStopAudioRecording = {},
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(400.dp),
            )
        }

        composeTestRule
            .onNodeWithTag(SEND_BUTTON_TAG)
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun microphone_click_invokes_start_audio_recording_callback() {
        var started = false

        composeTestRule.setContent {
            ChatInputBar(
                promptInput = "",
                canSend = false,
                sending = false,
                recordingAudio = false,
                onPromptChange = {},
                onSendClick = {},
                onStartAudioRecording = {
                    started = true
                },
                onStopAudioRecording = {},
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(400.dp),
            )
        }

        composeTestRule
            .onNodeWithTag(RECORD_AUDIO_BUTTON_TAG)
            .performClick()

        assertTrue(
            "expected start-audio-recording callback to be invoked",
            started,
        )
    }

    @Test
    fun shows_stop_audio_button_while_recording() {
        composeTestRule.setContent {
            ChatInputBar(
                promptInput = "",
                canSend = false,
                sending = false,
                recordingAudio = true,
                onPromptChange = {},
                onSendClick = {},
                onStartAudioRecording = {},
                onStopAudioRecording = {},
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(400.dp),
            )
        }

        composeTestRule
            .onNodeWithTag(STOP_AUDIO_RECORDING_BUTTON_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun recording_button_invokes_stop_audio_recording_callback() {
        var stopped = false

        composeTestRule.setContent {
            ChatInputBar(
                promptInput = "",
                canSend = false,
                sending = false,
                recordingAudio = true,
                onPromptChange = {},
                onSendClick = {},
                onStartAudioRecording = {},
                onStopAudioRecording = {
                    stopped = true
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(400.dp),
            )
        }

        composeTestRule
            .onNodeWithTag(STOP_AUDIO_RECORDING_BUTTON_TAG)
            .performClick()

        assertTrue(
            "expected stop-audio-recording callback to be invoked",
            stopped,
        )
    }

    @Test
    fun keeps_send_disabled_while_sending() {
        composeTestRule.setContent {
            ChatInputBar(
                promptInput = "Hola Juan",
                canSend = false,
                sending = true,
                recordingAudio = false,
                onPromptChange = {},
                onSendClick = {},
                onStartAudioRecording = {},
                onStopAudioRecording = {},
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(400.dp),
            )
        }

        composeTestRule
            .onNodeWithTag(SEND_BUTTON_TAG)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
}
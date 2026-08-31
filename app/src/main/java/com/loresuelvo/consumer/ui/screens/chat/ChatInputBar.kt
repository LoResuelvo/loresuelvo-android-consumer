package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R

/**
 * Bottom-of-screen prompt composer for the chat screen.
 *
 * Supports:
 * - Text messages
 * - Media attachment
 * - Audio recording
 *
 * Audio recording behaviour:
 * - Idle + empty prompt -> microphone button.
 * - Recording -> stop button.
 * - Empty prompt + not recording -> microphone starts recording.
 * - Recording button -> stops recording.
 *
 * The parent owns the actual recording lifecycle through:
 * [onStartAudioRecording]
 * [onStopAudioRecording]
 *
 * The component itself is stateless.
 */
@Composable
fun ChatInputBar(
    promptInput: String,
    canSend: Boolean,
    sending: Boolean,
    recordingAudio: Boolean,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onStartAudioRecording: () -> Unit,
    onStopAudioRecording: () -> Unit,
    onAttachClick: (() -> Unit)? = null,
    /**
     * Whether the audio affordance (Mic / Stop buttons) should
     * be rendered. Defaults to `true` so the chat-with-provider
     * surface keeps the existing behaviour. The AI diagnostic
     * chat surface passes `false` while the AI audio
     * functionality is not available (scenario 01-UXUI) so the
     * Mic / Stop buttons are omitted entirely and the field is
     * followed by either the Send button (when the prompt has
     * content) or empty space (when the prompt is blank).
     */
    audioEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 20.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ------------------------------------------------------------
        // Attach button
        // ------------------------------------------------------------

        if (onAttachClick != null && !recordingAudio) {
            Surface(
                onClick = onAttachClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(ATTACH_BUTTON_TAG),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(
                            R.string.conversation_attach_content_description,
                        ),
                        modifier = Modifier.testTag(ATTACH_ICON_TAG),
                    )
                }
            }
        }

        // ------------------------------------------------------------
        // Text input
        // ------------------------------------------------------------

        BasicTextField(
            value = promptInput,
            onValueChange = onPromptChange,
            enabled = !recordingAudio,
            modifier = Modifier
                .weight(1f)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                )
                .verticalScroll(rememberScrollState())
                .testTag(CHAT_INPUT_FIELD_TAG),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(
                MaterialTheme.colorScheme.primary,
            ),
            maxLines = CHAT_INPUT_MAX_LINES,
            singleLine = false,
            decorationBox = { inner ->
                if (promptInput.isEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.chat_input_placeholder,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                }

                inner()
            },
        )

        // ------------------------------------------------------------
        // Recording / Send button
        // ------------------------------------------------------------

        when {
            // Audio disabled -> the Mic / Stop affordances are
            // removed entirely (01-UXUI). The trailing slot shows
            // the Send button with its `canSend`-driven enabled
            // state, so the consumer sees a disabled button while
            // the prompt is empty and the live Send button as
            // soon as they type.
            !audioEnabled -> {
                SendButton(
                    canSend = canSend,
                    onSendClick = onSendClick,
                )
            }

            // Currently recording -> STOP
            recordingAudio -> {
                StopButton(
                    onStopAudioRecording = onStopAudioRecording,
                    enabled = true,
                )
            }

            // Empty prompt -> START RECORDING
            promptInput.isBlank() && !sending -> {
                MicButton(
                    onStartAudioRecording = onStartAudioRecording,
                    enabled = true,
                )
            }

            // Non-empty prompt -> SEND
            else -> {
                SendButton(
                    canSend = canSend,
                    onSendClick = onSendClick,
                )
            }
        }
    }
}

/**
 * Compose testTag for the prompt BasicTextField.
 */
const val CHAT_INPUT_FIELD_TAG: String = "chat_input-field"

/**
 * Trailing send button. Shared between the audio-enabled and
 * audio-disabled branches of [ChatInputBar] so the visual
 * styling and `testTag` stay in one place (extracted in 01-UXUI).
 */
@Composable
private fun SendButton(
    canSend: Boolean,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onSendClick,
        enabled = canSend,
        modifier = modifier
            .size(48.dp)
            .testTag(SEND_BUTTON_TAG),
        shape = CircleShape,
        color = if (canSend) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(
                alpha = 0.38f,
            )
        },
        contentColor = if (canSend) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimary.copy(
                alpha = 0.38f,
            )
        },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(
                    R.string.chat_send_content_description,
                ),
                modifier = Modifier.testTag(
                    SEND_ICON_TAG,
                ),
            )
        }
    }
}

/**
 * Trailing microphone button. Rendered with reduced opacity and
 * `enabled = false` when [audioEnabled] is `false` so the
 * consumer sees the affordance is present but unavailable
 * (scenario 01-UXUI). The button keeps its 48.dp footprint so
 * the layout doesn't shift between enabled / disabled states.
 */
@Composable
private fun MicButton(
    onStartAudioRecording: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onStartAudioRecording,
        enabled = enabled,
        modifier = modifier
            .size(48.dp)
            .testTag(RECORD_AUDIO_BUTTON_TAG),
        shape = CircleShape,
        color = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(
                alpha = 0.38f,
            )
        },
        contentColor = if (enabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimary.copy(
                alpha = 0.38f,
            )
        },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = stringResource(
                    R.string.conversation_record_audio_content_description,
                ),
                modifier = Modifier.testTag(
                    RECORD_AUDIO_ICON_TAG,
                ),
            )
        }
    }
}

/**
 * Trailing stop-recording button. Same disabled-styling rules
 * as [MicButton] so the slot keeps its footprint while the AI
 * audio feature is unavailable (01-UXUI).
 */
@Composable
private fun StopButton(
    onStopAudioRecording: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onStopAudioRecording,
        enabled = enabled,
        modifier = modifier
            .size(48.dp)
            .testTag(STOP_AUDIO_RECORDING_BUTTON_TAG),
        shape = CircleShape,
        color = if (enabled) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.error.copy(
                alpha = 0.38f,
            )
        },
        contentColor = if (enabled) {
            MaterialTheme.colorScheme.onError
        } else {
            MaterialTheme.colorScheme.onError.copy(
                alpha = 0.38f,
            )
        },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = stringResource(
                    R.string.conversation_stop_audio_recording_content_description,
                ),
                modifier = Modifier.testTag(
                    STOP_AUDIO_RECORDING_ICON_TAG,
                ),
            )
        }
    }
}

/**
 * Compose testTag for the trailing Send button.
 */
const val SEND_BUTTON_TAG: String = "chat-send-button"

/**
 * Compose testTag for the Send icon.
 */
const val SEND_ICON_TAG: String = "chat-send-icon"

/**
 * Compose testTag for the leading `+` attach button.
 */
const val ATTACH_BUTTON_TAG: String = "chat-attach-button"

/**
 * Compose testTag for the `+` icon.
 */
const val ATTACH_ICON_TAG: String = "chat-attach-icon"

/**
 * Compose testTag for the microphone button.
 */
const val RECORD_AUDIO_BUTTON_TAG: String = "chat-record-audio-button"

/**
 * Compose testTag for the microphone icon.
 */
const val RECORD_AUDIO_ICON_TAG: String = "chat-record-audio-icon"

/**
 * Compose testTag for the stop-recording button.
 */
const val STOP_AUDIO_RECORDING_BUTTON_TAG: String =
    "chat-stop-audio-recording-button"

/**
 * Compose testTag for the stop-recording icon.
 */
const val STOP_AUDIO_RECORDING_ICON_TAG: String =
    "chat-stop-audio-recording-icon"

/**
 * Compose testTag for the chat input divider.
 */
const val CHAT_INPUT_DIVIDER_TAG: String =
    "chat-input-divider"

/**
 * Maximum visible lines for the prompt field.
 */
const val CHAT_INPUT_MAX_LINES: Int = 6
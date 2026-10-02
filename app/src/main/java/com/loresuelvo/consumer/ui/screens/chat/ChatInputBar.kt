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
    state: ChatInputBarState,
    actions: ChatInputBarActions = ChatInputBarActions(),
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

        if (actions.onAttach != null && !state.recordingAudio) {
            Surface(
                onClick = requireNotNull(actions.onAttach),
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

        BasicTextField(
            value = state.promptInput,
            onValueChange = actions.onPromptChange,
            enabled = !state.recordingAudio,
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
                if (state.promptInput.isEmpty()) {
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

        when {

            !state.audioEnabled -> {
                SendButton(
                    canSend = state.canSend,
                    onSendClick = actions.onSend,
                )
            }

            state.recordingAudio -> {
                StopButton(
                    onStopAudioRecording = actions.onStopAudioRecording,
                    enabled = true,
                )
            }

            state.promptInput.isBlank() && !state.sending -> {
                MicButton(
                    onStartAudioRecording = actions.onStartAudioRecording,
                    enabled = true,
                )
            }

            else -> {
                SendButton(
                    canSend = state.canSend,
                    onSendClick = actions.onSend,
                )
            }
        }
    }
}

/**
 * Compose testTag for the prompt BasicTextField.
 */
const val CHAT_INPUT_FIELD_TAG: String = "chat_input-field"

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

package com.loresuelvo.consumer.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.ui.screens.chat.AudioPlaybackState

/**
 * WhatsApp-style bubble for a single message in a consumer ↔
 * provider conversation.
 *
 * Text messages render their content normally.
 * Image messages render the referenced image inside the bubble.
 * Audio messages render a compact audio representation with
 * playback controls and progress.
 */
@Composable
fun ConversationMessageBubble(
    message: ConversationMessage,
    audioPlayback: AudioPlaybackState = AudioPlaybackState(),
    onPlayAudio: (String) -> Unit = {},
    onPauseAudio: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isConsumer = message.sender is ConversationSender.Consumer

    val bubbleColor = if (isConsumer) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isConsumer) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isConsumer) {
            Arrangement.End
        } else {
            Arrangement.Start
        },
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag(CONVERSATION_MESSAGE_BUBBLE_TAG),
        ) {
            when (val media = message.media) {
                is MediaReference.Image -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(CONVERSATION_MESSAGE_IMAGE_TAG),
                    ) {
                        AsyncImage(
                            model = media.url,
                            contentDescription = media.originalName,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                is MediaReference.Audio -> {
                    val isCurrentAudio =
                        audioPlayback.messageId == message.id

                    val isThisPlaying =
                        isCurrentAudio && audioPlayback.isPlaying

                    val currentPositionMillis =
                        if (isCurrentAudio) {
                            audioPlayback.currentPositionMillis
                        } else {
                            0L
                        }

                    val progress = if (media.durationMillis > 0L) {
                        (
                            currentPositionMillis.toFloat() /
                                media.durationMillis.toFloat()
                        ).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    Column(
                        modifier = Modifier
                            .testTag(CONVERSATION_MESSAGE_AUDIO_TAG),
                    ) {
                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (isThisPlaying) PAUSE_ICON else PLAY_ICON,
                                color = textColor,
                                modifier = Modifier
                                    .testTag(CONVERSATION_MESSAGE_AUDIO_PLAY_TAG)
                                    .clickable {
                                        if (isThisPlaying) {
                                            onPauseAudio(message.id)
                                        } else {
                                            onPlayAudio(message.id)
                                        }
                                    },
                            )

                            Text(
                                text = formatAudioDuration(
                                    currentPositionMillis,
                                ),
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .testTag(
                                        CONVERSATION_MESSAGE_AUDIO_ELAPSED_TAG,
                                    ),
                            )

                            Text(
                                text = "/",
                                color = textColor,
                            )

                            Text(
                                text = formatAudioDuration(
                                    media.durationMillis,
                                ),
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .testTag(
                                        CONVERSATION_MESSAGE_AUDIO_DURATION_TAG,
                                    ),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    MaterialTheme.colorScheme
                                        .surfaceVariant,
                                )
                                .testTag(
                                    CONVERSATION_MESSAGE_AUDIO_PROGRESS_TAG,
                                ),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                    )
                                    .testTag(
                                        CONVERSATION_MESSAGE_AUDIO_FILL_TAG,
                                    ),
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        overflow = TextOverflow.Visible,
                    )
                }
            }
        }
    }
}

private fun formatAudioDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}

const val CONVERSATION_MESSAGE_BUBBLE_TAG =
    "conversation-message-bubble"

const val CONVERSATION_MESSAGE_IMAGE_TAG =
    "conversation-message-image"

const val CONVERSATION_MESSAGE_AUDIO_TAG =
    "conversation-message-audio"

const val CONVERSATION_MESSAGE_AUDIO_DURATION_TAG =
    "conversation-message-audio-duration"

const val CONVERSATION_MESSAGE_AUDIO_ELAPSED_TAG =
    "conversation-message-audio-elapsed"

const val CONVERSATION_MESSAGE_AUDIO_PLAY_TAG =
    "conversation-message-audio-play"

const val CONVERSATION_MESSAGE_AUDIO_PROGRESS_TAG =
    "conversation-message-audio-progress"

const val CONVERSATION_MESSAGE_AUDIO_FILL_TAG =
    "conversation-message-audio-fill"

internal const val PLAY_ICON = "\u25B6"
internal const val PAUSE_ICON = "\u23F8"
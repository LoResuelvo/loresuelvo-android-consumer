package com.loresuelvo.consumer.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.platform.media.AUDIO_WAVEFORM_BAR_COUNT
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.screens.chat.ConversationMessageBubbleActions
import com.loresuelvo.consumer.ui.screens.chat.ConversationMessageBubbleState

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
    state: ConversationMessageBubbleState,
    actions: ConversationMessageBubbleActions = ConversationMessageBubbleActions(),
    modifier: Modifier = Modifier,
) {
    val message = state.message
    val audioPlayback = state.audioPlayback
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
                .padding(8.dp)
                .testTag(CONVERSATION_MESSAGE_BUBBLE_TAG),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when (val media = message.media) {
                    is MediaReference.Image -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { actions.onImageClick(message.id) }
                                    .testTag(CONVERSATION_MESSAGE_IMAGE_TAG),
                                contentAlignment = Alignment.Center,
                            ) {
                                SubcomposeAsyncImage(
                                    model = media.url,
                                    contentDescription = media.originalName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    },
                                    error = {
                                        Icon(
                                            imageVector = Icons.Filled.BrokenImage,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                        )
                                    },
                                )
                            }
                            if (message.content.isNotBlank()) {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    overflow = TextOverflow.Visible,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp,
                                    ),
                                )
                            }
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
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isThisPlaying) {
                                            actions.onPauseAudio(message.id)
                                        } else {
                                            actions.onPlayAudio(message.id)
                                        }
                                    },
                                    modifier = Modifier.testTag(
                                        CONVERSATION_MESSAGE_AUDIO_PLAY_TAG,
                                    ).size(44.dp),
                                ) {
                                    Icon(
                                        imageVector = if (isThisPlaying) {
                                            Icons.Filled.Pause
                                        } else {
                                            Icons.Filled.PlayArrow
                                        },
                                        contentDescription = stringResource(
                                            if (isThisPlaying) {
                                                R.string.conversation_audio_pause_content_description
                                            } else {
                                                R.string.conversation_audio_play_content_description
                                            },
                                        ),
                                        tint = textColor,
                                    )
                                }

                                AudioWaveform(
                                    progress = progress,
                                    color = textColor,
                                    amplitudes = media.waveform,
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 52.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = formatAudioDuration(
                                        currentPositionMillis,
                                    ),
                                    color = textColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.testTag(
                                        CONVERSATION_MESSAGE_AUDIO_ELAPSED_TAG,
                                    ),
                                )

                                Text(
                                    text = "/",
                                    color = textColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 2.dp),
                                )

                                Text(
                                    text = formatAudioDuration(media.durationMillis),
                                    color = textColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.testTag(
                                        CONVERSATION_MESSAGE_AUDIO_DURATION_TAG,
                                    ),
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                ConversationMessageTimestamp(
                                    epochMillis = message.createdOnEpochMillis,
                                    color = textColor,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }

                    is MediaReference.Video -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConversationVideoPlayer(
                                media = media,
                                onOpenFullscreen = { actions.onVideoClick(message.id) },
                            )
                            if (message.content.isNotBlank()) {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    overflow = TextOverflow.Visible,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp,
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

                if (message.media !is MediaReference.Audio) {
                    ConversationMessageTimestamp(
                        epochMillis = message.createdOnEpochMillis,
                        color = textColor,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioWaveform(
    progress: Float,
    color: androidx.compose.ui.graphics.Color,
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
) {
    val bars = amplitudes.ifEmpty {
        List(AUDIO_WAVEFORM_BAR_COUNT) { DEFAULT_WAVEFORM_AMPLITUDE }
    }

    Box(
        modifier = modifier
            .height(32.dp)
            .testTag(CONVERSATION_MESSAGE_AUDIO_PROGRESS_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(CONVERSATION_MESSAGE_AUDIO_WAVEFORM_TAG),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bars.forEachIndexed { index, amplitude ->
                    val isPlayed = index < progress * bars.size
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(amplitude)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                color = color.copy(
                                    alpha = if (isPlayed) 0.95f else 0.32f,
                                ),
                            ),
                    )
                }
            }
        }

        // Keeps the progress geometry available to tests and accessibility
        // tooling while the waveform itself communicates the progress.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .testTag(CONVERSATION_MESSAGE_AUDIO_FILL_TAG),
        )
    }
}

@Composable
private fun ConversationMessageTimestamp(
    epochMillis: Long,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val timestamp = formatConversationMessageTime(epochMillis)
    if (timestamp.isNotEmpty()) {
        Text(
            text = timestamp,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.72f),
            modifier = modifier
                .testTag(CONVERSATION_MESSAGE_TIMESTAMP_TAG),
        )
    }
}

internal fun formatConversationMessageTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    return java.text.DateFormat
        .getTimeInstance(java.text.DateFormat.SHORT, java.util.Locale.getDefault())
        .format(java.util.Date(epochMillis))
}

private fun formatAudioDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}

const val CONVERSATION_MESSAGE_BUBBLE_TAG =
    "conversation-message-bubble"

const val CONVERSATION_MESSAGE_TIMESTAMP_TAG =
    "conversation-message-timestamp"

const val CONVERSATION_MESSAGE_IMAGE_TAG =
    "conversation-message-image"

const val CONVERSATION_MESSAGE_IMAGE_CONTENT_TAG =
    "conversation-message-image-content"

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

const val CONVERSATION_MESSAGE_AUDIO_WAVEFORM_TAG =
    "conversation-message-audio-waveform"

private const val DEFAULT_WAVEFORM_AMPLITUDE = 0.45f

internal const val PLAY_ICON = "\u25B6"
internal const val PAUSE_ICON = "\u23F8"

package com.loresuelvo.consumer.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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

/**
 * WhatsApp-style bubble for a single message in a consumer ↔
 * provider conversation.
 *
 * Text messages render their content normally.
 * Image messages render the referenced image inside the bubble.
 */
@Composable
fun ConversationMessageBubble(
    message: ConversationMessage,
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
                    AsyncImage(
                        model = media.url,
                        contentDescription = media.originalName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(CONVERSATION_MESSAGE_IMAGE_TAG),
                    )
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

const val CONVERSATION_MESSAGE_BUBBLE_TAG =
    "conversation-message-bubble"

const val CONVERSATION_MESSAGE_IMAGE_TAG =
    "conversation-message-image"
package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Sender

/**
 * Stateless message bubble. Dispatches to a consumer (right-aligned,
 * primary colour) or assistant (left-aligned, surface colour + 32dp
 * avatar) variant based on [ChatMessage.sender]. Exhaustive `when`
 * enforces both branches at compile time and protects future
 * subtypes of [Sender].
 *
 * The asymmetric top corner (top-start for the assistant, top-end
 * for the consumer) reads as a "conversational tail" and matches
 * modern chat UIs (iMessage, ChatGPT).
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    when (message.sender) {
        Sender.Consumer -> ConsumerBubble(message = message, modifier = modifier)
        Sender.Assistant -> AssistantBubble(message = message, modifier = modifier)
    }
}

@Composable
private fun ConsumerBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 4.dp,
                        bottomEnd = 20.dp,
                        bottomStart = 20.dp,
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun AssistantBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    // Avatar on the left, bubble on the right. The avatar is 32dp
    // so it reads as a "speaker" without competing with the bubble
    // text (matches WhatsApp, ChatGPT, Gemini).
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistantAvatar()
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 20.dp,
                        bottomEnd = 20.dp,
                        bottomStart = 20.dp,
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
            )
        }
    }
}

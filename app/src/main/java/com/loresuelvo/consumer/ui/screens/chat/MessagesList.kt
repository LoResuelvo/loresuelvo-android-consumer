package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage

/**
 * Lazy list of chat messages. Renders each [ChatMessage] through
 * [MessageBubble] in arrival order, using the message's stable
 * `id` as the LazyColumn key.
 *
 * When [typingIndicatorVisible] is `true`, an extra item
 * ([TypingIndicatorBubble]) is appended to the list so the user
 * sees the assistant's "escribiendo…" hint while a round-trip is
 * in flight (scenario 03-DIA). The bubble uses a fixed key so
 * Compose doesn't tear it down on recomposition.
 */
@Composable
fun MessagesList(
    messages: List<ChatMessage>,
    typingIndicatorVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = messages, key = { it.id }) { message ->
            MessageBubble(message = message)
        }
        if (typingIndicatorVisible) {
            item(key = TYPING_INDICATOR_KEY) {
                TypingIndicatorBubble()
            }
        }
    }
}

private const val TYPING_INDICATOR_KEY: String = "chat-typing-indicator"

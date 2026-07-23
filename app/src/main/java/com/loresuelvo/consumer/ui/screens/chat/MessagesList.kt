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
 * Conditional items appended in order:
 *
 *  - [TypingIndicatorBubble] when [typingIndicatorVisible] is
 *    `true` (in-flight round-trip, scenario 03-DIA).
 *  - [ChatErrorCard] when [transientError] is non-null (failed
 *    round-trip, scenario 04-DIA). Retry + dismiss callbacks are
 *    forwarded verbatim — both flow back through the
 *    [com.loresuelvo.consumer.ui.screens.chat.ChatViewModel].
 *
 * All conditional items use stable keys so Compose doesn't tear
 * them down across recompositions (typing indicator → success, or
 * error card → retry → success).
 */
@Composable
fun MessagesList(
    messages: List<ChatMessage>,
    typingIndicatorVisible: Boolean,
    transientError: ChatError?,
    onRetryClick: () -> Unit,
    onErrorDismissClick: () -> Unit,
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
        if (transientError != null) {
            item(key = ERROR_CARD_KEY) {
                ChatErrorCard(
                    error = transientError,
                    onRetryClick = onRetryClick,
                    onDismissClick = onErrorDismissClick,
                )
            }
        }
    }
}

private const val TYPING_INDICATOR_KEY: String = "chat-typing-indicator"
private const val ERROR_CARD_KEY: String = "chat-error-card"

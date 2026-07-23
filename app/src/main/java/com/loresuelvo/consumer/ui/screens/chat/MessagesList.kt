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
 * [MessageBubble] in arrival order, using the message's stable `id`
 * as the LazyColumn key.
 *
 * Auto-scrolling to the newest message lands in a later commit; for
 * scenario 01-DIA the consumer sends the first message and the list
 * has one entry, so no scrolling is needed.
 */
@Composable
fun MessagesList(
    messages: List<ChatMessage>,
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
    }
}

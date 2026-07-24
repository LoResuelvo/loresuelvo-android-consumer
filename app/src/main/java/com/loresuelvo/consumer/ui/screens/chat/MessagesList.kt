package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage

/**
 * Pure mapping from the current state to the index the messages
 * list should be auto-scrolled to. The "scroll to last" pattern
 * is the canonical chat-app UX (ChatGPT, WhatsApp, iMessage all
 * implement it the same way):
 *
 *  - empty state → no scroll target (returns `null`).
 *  - messages present, no typing indicator → last user/assistant
 *    item.
 *  - messages present, typing indicator visible → the LAST slot is
 *    the typing bubble (item index = `messages.size`), so the
 *    spinner stays on screen while the round-trip is in flight.
 *
 * Extracted as a top-level internal function so the math can be
 * unit-tested without spinning up Compose / Robolectric. The
 * Composable's [LaunchedEffect] is a thin wrapper that calls
 * [LazyListState.scrollToItem] with the result.
 *
 * Ticket 1 of the chat-UX backlog: auto-scroll on send and on
 * receive (typed message → optimistic bubble, server reply → full
 * history).
 */
internal fun messagesListScrollIndex(
    messageCount: Int,
    typingIndicatorVisible: Boolean,
): Int? {
    if (messageCount == 0) return null
    return if (typingIndicatorVisible) messageCount else messageCount - 1
}

/**
 * "Respect reader position" gate (ticket 4 of the chat-UX backlog).
 *
 * Returns `true` iff the [MessagesList] should
 * [androidx.compose.foundation.lazy.LazyListState.scrollToItem] to
 * the freshly-computed [target]. The rule: the auto-scroll only
 * fires when the user is already at the bottom of the list, so a
 * brand-new assistant reply does not yank the reader away from
 * older messages they're currently scrolling through. The chat
 * itself still scrolls UPWARD via the user — they just don't get
 * force-scrolled down any time a new bubble arrives.
 *
 * Pure function for unit testing; the Composable reads this via
 * [androidx.compose.runtime.derivedStateOf] over
 * `listState.layoutInfo`.
 */
internal fun shouldAutoScroll(target: Int?, isAtBottom: Boolean): Boolean =
    target != null && isAtBottom

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
 *
 * The [listState] is exposed as a parameter so the
 * `MessagesListTest` can capture it and assert the auto-scroll
 * behaviour landed at the last item. In production the default
 * [rememberLazyListState] is the right choice.
 */
@Composable
fun MessagesList(
    messages: List<ChatMessage>,
    typingIndicatorVisible: Boolean,
    transientError: ChatError?,
    onRetryClick: () -> Unit,
    onErrorDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    // Auto-scroll on every new message + typing indicator change so
    // the user's newest prompt and the assistant's in-flight /
    // final bubble are always on screen **if the user is already
    // at the bottom**. If the user has scrolled up to read older
    // messages, the new bubble just appears below — no yank — so
    // their reading position is preserved (ticket 4).
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount == 0 ||
                info.visibleItemsInfo.lastOrNull()?.index == info.totalItemsCount - 1
        }
    }
    LaunchedEffect(messages.size, typingIndicatorVisible) {
        val target = messagesListScrollIndex(
            messageCount = messages.size,
            typingIndicatorVisible = typingIndicatorVisible,
        )
        // `shouldAutoScroll` is a non-inline pure function so
        // Kotlin can't smart-cast `target` after the call. Check
        // explicitly so the resulting `Int` reaches `scrollToItem`.
        if (target != null && shouldAutoScroll(target, isAtBottom)) {
            listState.scrollToItem(target)
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
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

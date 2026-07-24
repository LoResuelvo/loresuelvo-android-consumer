package com.loresuelvo.consumer.ui.screens.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [messagesListScrollIndex]. The pure mapping
 * decides what index [MessagesList] should auto-scroll to. The
 * integration with `LazyListState.scrollToItem(...)` is exercised
 * by the Compose runtime in production and verified by manual
 * smoke tests; the math is the only rigorous contract and that's
 * what this class covers.
 *
 * Ticket 1 of the chat-UX backlog.
 */
class MessagesListScrollIndexTest {

    @Test
    fun empty_conversation_returns_null_target() {
        assertNull(messagesListScrollIndex(messageCount = 0, typingIndicatorVisible = false))
        assertNull(messagesListScrollIndex(messageCount = 0, typingIndicatorVisible = true))
    }

    @Test
    fun one_or_more_messages_with_no_indicator_returns_last_message_index() {
        assertEquals(0, messagesListScrollIndex(messageCount = 1, typingIndicatorVisible = false))
        assertEquals(1, messagesListScrollIndex(messageCount = 2, typingIndicatorVisible = false))
        assertEquals(4, messagesListScrollIndex(messageCount = 5, typingIndicatorVisible = false))
    }

    @Test
    fun typing_indicator_visible_pushes_target_one_past_last_message() {
        // The indicator is appended as the next item AFTER all
        // existing messages, so the index to scroll to is the
        // current message count (i.e. the slot the indicator will
        // occupy).
        assertEquals(1, messagesListScrollIndex(messageCount = 1, typingIndicatorVisible = true))
        assertEquals(2, messagesListScrollIndex(messageCount = 2, typingIndicatorVisible = true))
        assertEquals(5, messagesListScrollIndex(messageCount = 5, typingIndicatorVisible = true))
    }

    @Test
    fun boundary_between_messages_and_indicator_does_not_double_offset() {
        // Concretely: with 2 messages + indicator visible, the
        // indicator lives at index 2 — that's exactly `messageCount`,
        // not `messageCount + 1` (which would scroll past it).
        assertEquals(2, messagesListScrollIndex(messageCount = 2, typingIndicatorVisible = true))
    }
}

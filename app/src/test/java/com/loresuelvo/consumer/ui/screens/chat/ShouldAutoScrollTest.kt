package com.loresuelvo.consumer.ui.screens.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [shouldAutoScroll]. The helper encodes the
 * "respect reader position" rule (ticket 4 of the chat-UX
 * backlog): auto-scroll to the bottom on a new message **only**
 * when the user is already at the bottom, so a fresh server reply
 * doesn't yank the reader away from older messages they're
 * currently reading.
 *
 * The truth table:
 *  - `target == null` (empty state) → never scroll.
 *  - `target != null` AND `isAtBottom == true` → scroll.
 *  - `target != null` AND `isAtBottom == false` → do NOT scroll.
 */
class ShouldAutoScrollTest {

    @Test
    fun null_target_never_scrolls() {
        assertFalse(shouldAutoScroll(target = null, isAtBottom = true))
        assertFalse(shouldAutoScroll(target = null, isAtBottom = false))
    }

    @Test
    fun non_null_target_scrolls_when_at_bottom() {
        assertTrue(shouldAutoScroll(target = 1, isAtBottom = true))
        assertTrue(shouldAutoScroll(target = 5, isAtBottom = true))
    }

    @Test
    fun non_null_target_does_not_scroll_when_user_read_above() {
        assertFalse(shouldAutoScroll(target = 1, isAtBottom = false))
        assertFalse(shouldAutoScroll(target = 5, isAtBottom = false))
    }
}

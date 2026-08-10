package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome

/**
 * UDF state for the consumer ↔ provider conversation detail
 * screen (`Route.Conversation`). Modelled as a sealed hierarchy
 * so the screen renders exactly one of the states without
 * boolean flags — mirrors
 * [com.loresuelvo.consumer.ui.screens.messages.MessagesListUiState]
 * and the AI diagnostic's
 * [com.loresuelvo.consumer.ui.screens.chat.ChatUiState].
 *
 *  - [Loading] — initial fetch of the conversation detail in
 *    flight.
 *  - [Ready] — detail loaded; the composer is live and the user
 *    may send messages. The conversation can be in any
 *    `ConversationStatus` (Pending / Accepted / Other) — the
 *    composer is NOT gated on acceptance (scenario 05-IC
 *    asserts "without restrictions"). The screen renders a
 *    "Pendiente" badge in the top bar when the conversation is
 *    still awaiting the provider's acceptance.
 *  - [Error] — the initial detail fetch failed. The screen
 *    renders the typed failure copy and a retry button. Sending
 *    a message while in this state is a no-op (the composer is
 *    not rendered).
 *
 * Once in [Ready], transient send failures land in
 * [Ready.transientError] (a card pinned above the composer with
 * a retry CTA). The last attempted prompt is snapshotted in
 * [Ready.lastAttemptedPrompt] so the retry CTA can resubmit
 * without the user re-typing.
 *
 * Real-time incoming messages (scenarios 09-IC / 10-IC) are
 * driven by two flags:
 *  - [Ready.isAtBottom] — whether the LazyList is currently
 *    scrolled to the last visible item (true) or the user has
 *    scrolled up reading older messages (false). The screen
 *    reports this back via [ConversationViewModel.onScrollPositionChanged].
 *  - [Ready.hasUnreadIncoming] — `true` when a new provider
 *    message arrived while the user was scrolled up; the screen
 *    renders a "↓ nuevo mensaje" banner the user can tap to jump
 *    to the bottom. Cleared when the user scrolls back to the
 *    bottom OR when a fresh message arrives while they're
 *    already at the bottom.
 */
sealed interface ConversationUiState {

    data object Loading : ConversationUiState

    data class Ready(
        val detail: ConversationDetail,
        val promptInput: String,
        val sending: Boolean,
        val transientError: SendMessageOutcome.Failure? = null,
        val lastAttemptedPrompt: String? = null,
        val isAtBottom: Boolean = true,
        val hasUnreadIncoming: Boolean = false,
    ) : ConversationUiState

    data class Error(val failure: ConversationDetailOutcome.Failure) : ConversationUiState
}
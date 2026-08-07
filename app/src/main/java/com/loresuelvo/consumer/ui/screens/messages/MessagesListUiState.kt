package com.loresuelvo.consumer.ui.screens.messages

import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome

/**
 * UDF state for the consumer's conversations list
 * (`Route.Messages`). Modelled as a sealed hierarchy so the screen
 * renders exactly one of the states without boolean flags —
 * mirrors `ProfessionalsUiState`.
 *
 * The list cell rendering lives inside `Ready`: a non-empty
 * `conversations` renders the LazyColumn, an empty list renders
 * the "no conversations yet" empty-state card. We deliberately
 * do NOT carry a separate `Empty` state — the VM's contract is
 * "the round-trip succeeded"; whether the consumer has any
 * conversations is a presentation concern.
 *
 *  - [Loading] — initial fetch in flight (also reused on manual
 *    retry).
 *  - [Ready] — round-trip succeeded; [conversations] may be empty.
 *  - [Error] — round-trip failed; the carried
 *    [ConversationsOutcome.Failure] subtype lets the screen render
 *    network vs server vs unauthorized strings distinctly. Pull-to-
 *    refresh / retry lands in a follow-up; for now `load()`
 *    exposes the same code path.
 */
sealed interface MessagesListUiState {

    data object Loading : MessagesListUiState

    data class Ready(val conversations: List<Conversation>) : MessagesListUiState

    data class Error(val failure: ConversationsOutcome.Failure) : MessagesListUiState
}
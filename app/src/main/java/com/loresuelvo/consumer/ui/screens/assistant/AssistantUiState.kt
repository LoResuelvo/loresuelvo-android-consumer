package com.loresuelvo.consumer.ui.screens.assistant

import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.domain.assistant.AiConversationSummary

/**
 * UDF state for the "Asistente IA" tab. Mirrors the convention
 * used by `MessagesListUiState` for the consumer ↔ provider
 * conversation list:
 *
 *  - [Loading]: the initial state when the screen is first
 *    composed; the VM spins the slow `GET /chatbot/conversations`
 *    round-trip.
 *  - [Ready]: the round-trip succeeded with a non-empty list.
 *    The screen renders a `LazyColumn` of session rows.
 *  - [Empty]: the round-trip succeeded but the list is empty
 *    (no past AI sessions). The screen renders an empty-state
 *    card so the consumer isn't staring at a blank surface.
 *  - [Error]: the round-trip failed. Three typed sub-cases are
 *    rendered with localised copy via
 *    `R.string.assistant_screen_error_*`.
 *
 * The split between [Empty] and [Ready] keeps the screen
 * readable: the consumer must see a clear "you haven't started
 * yet" hint, not a 0-row list that could be confused for a
 * stale render.
 */
sealed interface AssistantUiState {

    data object Loading : AssistantUiState

    data class Ready(
        val conversations: List<AiConversationSummary>,
    ) : AssistantUiState

    data object Empty : AssistantUiState

    /**
     * Typed failure surface for the "Asistente IA" list. Mirrors
     * the consumer ↔ provider messages error pattern
     * (`ConversationsOutcome.Failure`): a sealed sub-hierarchy so
     * the UI can render localised copy per branch and the VM
     * keeps the typed discipline.
     */
    sealed interface Failure : AssistantUiState {
        data object Network : Failure
        data class Server(val message: String) : Failure
        data object Unauthorized : Failure
    }
}

/**
 * Maps the repository's typed outcome to the screen's typed state.
 * Pure helper so the VM's `load()` body stays a "dispatch on
 * outcome" block — the branch logic is testable in isolation.
 */
internal fun AiConversationListOutcome.toUiState(): AssistantUiState =
    when (this) {
        is AiConversationListOutcome.Success ->
            if (conversations.isEmpty()) AssistantUiState.Empty
            else AssistantUiState.Ready(conversations)

        is AiConversationListOutcome.Failure.Network ->
            AssistantUiState.Failure.Network
        is AiConversationListOutcome.Failure.Server ->
            AssistantUiState.Failure.Server(message)
        is AiConversationListOutcome.Failure.Unauthorized ->
            AssistantUiState.Failure.Unauthorized
    }

package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal

/**
 * UDF state for the proposal-summary card rendered above the
 * message list on the consumer ↔ provider conversation screen
 * (US-54 scenario 14-VSP). Modelled as a sealed hierarchy so the
 * screen renders exactly one branch without boolean flags:
 *
 *  - [Loading] — initial fetch in flight. The card stays
 *    hidden; the chat composer and messages render normally.
 *  - [Ready] — the conversation is linked to a service proposal;
 *    the card carries the full [ServiceProposal] snapshot the
 *    screen needs to render monto, fecha, descripción y estado.
 *  - [Empty] — no proposal is linked to this conversation (the
 *    conversation pre-dates the proposal feature, or the backend
 *    round trip failed and we silently fell back to "no summary"
 *    so the chat keeps working). The card stays hidden.
 */
sealed interface ConversationProposalSummaryUiState {
    data object Loading : ConversationProposalSummaryUiState
    data class Ready(val proposal: ServiceProposal) : ConversationProposalSummaryUiState
    data object Empty : ConversationProposalSummaryUiState
}
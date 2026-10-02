package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal

sealed interface ConversationProposalSummaryUiState {
    data object Loading : ConversationProposalSummaryUiState
    data class Ready(val proposal: ServiceProposal) : ConversationProposalSummaryUiState
    data object Empty : ConversationProposalSummaryUiState
}
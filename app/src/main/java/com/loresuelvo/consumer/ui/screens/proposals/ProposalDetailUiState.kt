package com.loresuelvo.consumer.ui.screens.proposals

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome

sealed interface ProposalDetailUiState {
    /**
     * The default state when no proposal has been requested yet, or
     * the consumer has dismissed the bottom sheet and the host
     * called [com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailViewModel.reset].
     * The sheet stays hidden in this state so a stale `Error` from
     * a post-dismiss round trip can never re-open the modal on its
     * own.
     */
    data object Idle : ProposalDetailUiState
    data object Loading : ProposalDetailUiState
    data class Ready(val proposal: ServiceProposal) : ProposalDetailUiState
    data object Unavailable : ProposalDetailUiState
    data class Error(val failure: ServiceProposalsOutcome.Failure) : ProposalDetailUiState
}

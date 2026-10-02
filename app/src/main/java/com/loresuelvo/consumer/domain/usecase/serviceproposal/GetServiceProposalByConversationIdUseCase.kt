package com.loresuelvo.consumer.domain.usecase.serviceproposal

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import javax.inject.Inject

sealed interface GetServiceProposalByConversationIdOutcome {
    data class Linked(val proposal: ServiceProposal) : GetServiceProposalByConversationIdOutcome
    data object NotLinked : GetServiceProposalByConversationIdOutcome
    data class Failure(val failure: ServiceProposalsOutcome.Failure) : GetServiceProposalByConversationIdOutcome
}

class GetServiceProposalByConversationIdUseCase @Inject constructor(
    private val repository: ServiceProposalRepository,
) {
    suspend operator fun invoke(
        conversationId: String,
    ): GetServiceProposalByConversationIdOutcome =
        when (val outcome = repository.getServiceProposals()) {
            is ServiceProposalsOutcome.Success -> {
                val match = outcome.proposals
                    .firstOrNull { it.conversationId == conversationId }
                if (match != null) {
                    GetServiceProposalByConversationIdOutcome.Linked(match)
                } else {
                    GetServiceProposalByConversationIdOutcome.NotLinked
                }
            }
            is ServiceProposalsOutcome.Failure ->
                GetServiceProposalByConversationIdOutcome.Failure(outcome)
        }
}
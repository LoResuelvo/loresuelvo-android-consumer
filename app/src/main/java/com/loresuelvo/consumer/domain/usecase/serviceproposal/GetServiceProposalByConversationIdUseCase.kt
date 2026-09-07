package com.loresuelvo.consumer.domain.usecase.serviceproposal

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import javax.inject.Inject

/**
 * Outcome for [GetServiceProposalByConversationIdUseCase]. Models
 * the three observable branches the conversation-summary surface
 * (US-54 scenario 14-VSP) needs:
 *
 *  - [Linked]: a proposal tied to the conversation id was found;
 *    the conversation renders the summary card on top of the
 *    message list.
 *  - [NotLinked]: no proposal is tied to the conversation id
 *    (either the conversation predates the proposal feature or
 *    the backend filtered it out); the conversation renders
 *    normally without a summary card.
 *  - [Failure]: the round trip failed; the screen falls back to
 *    "no summary" so the chat is never blocked by the proposal
 *    fetch — same reason the AI-diagnostic chat surfaces its own
 *    errors instead of inheriting the proposal repository's
 *    failure hierarchy.
 */
sealed interface GetServiceProposalByConversationIdOutcome {
    data class Linked(val proposal: ServiceProposal) : GetServiceProposalByConversationIdOutcome
    data object NotLinked : GetServiceProposalByConversationIdOutcome
    data class Failure(val failure: ServiceProposalsOutcome.Failure) : GetServiceProposalByConversationIdOutcome
}

/**
 * Looks up the [ServiceProposal] tied to a conversation. Used by
 * the consumer ↔ provider conversation screen (US-54 scenario
 * 14-VSP) to render the summary card on top of the message list.
 *
 * The lookup goes through [ServiceProposalRepository.getServiceProposals]
 * and filters locally by `conversationId`. The repo does not yet
 * expose a server-side `GET /service-proposals/by-conversation/{id}`
 * endpoint, so the consumer screen pays one extra round trip per
 * conversation mount. When the dedicated endpoint lands the
 * use case can switch to it without changing the outcome shape.
 *
 * The use case **never throws** on backend failures — failures
 * collapse to [GetServiceProposalByConversationIdOutcome.Failure]
 * so the caller can decide whether to surface, log, or ignore the
 * problem (the conversation screen currently chooses to ignore).
 */
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
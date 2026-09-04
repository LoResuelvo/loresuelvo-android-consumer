package com.loresuelvo.consumer.domain.usecase.serviceproposal

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the consumer's service proposals through the
 * [ServiceProposalRepository] port and keeps only the entries in
 * [ServiceProposalStatus.Accepted] — the "trabajos próximos a
 * realizarse" surfaced on the Home dashboard (US-54 scenario
 * 02-VSP).
 *
 * The "accepted" filter lives in the use case (not in the
 * repository) for the same reason as
 * [GetPendingServiceProposalsUseCase]: the same endpoint feeds
 * the Home ("upcoming jobs" surface), Mis Servicios (all
 * statuses) and the future work-order surfaces, each with its
 * own filter. Stateless; failures are propagated verbatim.
 */
@Singleton
class GetAcceptedServiceProposalsUseCase @Inject constructor(
    private val repository: ServiceProposalRepository,
) {
    suspend operator fun invoke(): ServiceProposalsOutcome =
        when (val outcome = repository.getServiceProposals()) {
            is ServiceProposalsOutcome.Success ->
                ServiceProposalsOutcome.Success(
                    proposals = outcome.proposals.filter {
                        it.status == ServiceProposalStatus.Accepted
                    },
                )
            is ServiceProposalsOutcome.Failure -> outcome
        }
}

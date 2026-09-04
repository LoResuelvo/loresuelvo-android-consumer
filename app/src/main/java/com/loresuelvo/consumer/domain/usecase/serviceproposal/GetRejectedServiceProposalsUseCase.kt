package com.loresuelvo.consumer.domain.usecase.serviceproposal

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the consumer's service proposals through the
 * [ServiceProposalRepository] port and keeps only the entries in
 * [ServiceProposalStatus.Rejected]. Powers the "Rechazadas" filter
 * chip on the MisServicios surface (US-54 scenario 07-VSP).
 *
 * Mirrors [GetPendingServiceProposalsUseCase] and
 * [GetAcceptedServiceProposalsUseCase] — same thin orchestrator
 * shape, same failure-propagation contract. Stateless; the
 * "Rechazadas" filter is applied at the use case boundary so the
 * VM never has to know about it directly.
 */
@Singleton
class GetRejectedServiceProposalsUseCase @Inject constructor(
    private val repository: ServiceProposalRepository,
) {
    suspend operator fun invoke(): ServiceProposalsOutcome =
        when (val outcome = repository.getServiceProposals()) {
            is ServiceProposalsOutcome.Success ->
                ServiceProposalsOutcome.Success(
                    proposals = outcome.proposals.filter {
                        it.status == ServiceProposalStatus.Rejected
                    },
                )
            is ServiceProposalsOutcome.Failure -> outcome
        }
}

package com.loresuelvo.consumer.domain.usecase.serviceproposal

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the consumer's service proposals through the
 * [ServiceProposalRepository] port and keeps only the entries in
 * [ServiceProposalStatus.Pending].
 *
 * The "pending" filter lives in the use case (not in the
 * repository) because it is a Home-specific projection: the same
 * endpoint feeds Mis Servicios (scenario 03-VSP), which shows
 * every status, and the future work-order surfaces. Each consumer
 * applies its own filter so the wire contract stays narrow
 * (one endpoint, no query params).
 *
 * Stateless; failures are propagated verbatim. The use case never
 * swallows a [ServiceProposalsOutcome.Failure] into an empty
 * `Success` — the ViewModel needs the typed failure to decide
 * whether to retry, render an error, or stay in `Ready`.
 */
@Singleton
class GetPendingServiceProposalsUseCase @Inject constructor(
    private val repository: ServiceProposalRepository,
) {
    suspend operator fun invoke(): ServiceProposalsOutcome =
        when (val outcome = repository.getServiceProposals()) {
            is ServiceProposalsOutcome.Success ->
                ServiceProposalsOutcome.Success(
                    proposals = outcome.proposals.filter {
                        it.status == ServiceProposalStatus.Pending
                    },
                )
            is ServiceProposalsOutcome.Failure -> outcome
        }
}

package com.loresuelvo.consumer.domain.usecase.serviceproposal

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the consumer's service proposals through the
 * [ServiceProposalRepository] port and surfaces the result
 * **sorted by recency, newest first** (US-54 scenario 04-VSP).
 *
 * The "no filter + chronological order" bundle is the
 * MisServicios contract: scenarios 05-VSP / 06-VSP / 07-VSP
 * layer the per-status filter on top of this list, scenarios
 * 04-VSP and 03-VSP rely on the recency sort. Both decisions
 * live in the use case (not in the VM nor in the repository)
 * so the wire contract stays narrow and the future work-order
 * surface can reuse the raw repository list without inheriting
 * this view-side ordering.
 *
 * Failures are propagated verbatim. `sortedByDescending` is a
 * stable sort, so two proposals sharing the same
 * `createdOnEpochMillis` keep their insertion order — pinned by
 * the dedicated "stable" test in the use case suite.
 */
@Singleton
class GetAllServiceProposalsUseCase @Inject constructor(
    private val repository: ServiceProposalRepository,
) {
    suspend operator fun invoke(): ServiceProposalsOutcome =
        when (val outcome = repository.getServiceProposals()) {
            is ServiceProposalsOutcome.Success ->
                ServiceProposalsOutcome.Success(
                    proposals = outcome.proposals.sortedByDescending {
                        it.createdOnEpochMillis
                    },
                )
            is ServiceProposalsOutcome.Failure -> outcome
        }
}

package com.loresuelvo.consumer.domain.usecase.serviceproposal

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import javax.inject.Inject
import javax.inject.Singleton

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

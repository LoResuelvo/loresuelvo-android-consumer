package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeServiceProposalRepository @Inject constructor() : ServiceProposalRepository {
    private var seed: List<ServiceProposal> = emptyList()

    fun set(proposals: List<ServiceProposal>) {
        seed = proposals
    }

    override suspend fun getServiceProposals(): ServiceProposalsOutcome =
        ServiceProposalsOutcome.Success(seed)
}

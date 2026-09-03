package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test-only [ServiceProposalRepository] for the acceptance /
 * integration tests that `@UninstallModules(RepositoryModule::class)`.
 *
 * The production binding lives in
 * [com.loresuelvo.consumer.di.RepositoryModule]; once a test
 * uninstalls that module it must rebind every port the ViewModels
 * under test transitively depend on — including
 * [ServiceProposalRepository], which the new US-54
 * `HomeViewModel` constructor pulls in via
 * [com.loresuelvo.consumer.domain.usecase.serviceproposal.GetPendingServiceProposalsUseCase].
 *
 * Behaviour: `getServiceProposals` → empty `Success` (the consumer
 * has no proposals pending today). Each test that needs a
 * different seed should subclass or replace this fake with one
 * that stubs the round trip explicitly.
 */
@Singleton
class FakeServiceProposalRepository @Inject constructor() : ServiceProposalRepository {
    override suspend fun getServiceProposals(): ServiceProposalsOutcome =
        ServiceProposalsOutcome.Success(emptyList<ServiceProposal>())
}

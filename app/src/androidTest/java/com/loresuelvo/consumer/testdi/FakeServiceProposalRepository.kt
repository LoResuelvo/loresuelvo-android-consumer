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
 * [ServiceProposalRepository], which `HomeViewModel` pulls in via
 * [com.loresuelvo.consumer.domain.usecase.serviceproposal.GetPendingServiceProposalsUseCase]
 * (and the upcoming / all variants added in US-54 scenarios 02-VSP
 * and 03-VSP).
 *
 * Default seed: empty list. Instrumented tests that need a
 * populated list call [set] from the test thread before
 * `scenario.recreate()` so the new seed lands in the same
 * `@Singleton` instance the activity resolves through
 * `hiltViewModel()`. The setter MUST run on the main thread (via
 * `composeTestRule.runOnUiThread { ... }`) so the Hilt graph sees
 * the change before the activity's VM is constructed.
 */
@Singleton
class FakeServiceProposalRepository @Inject constructor() : ServiceProposalRepository {
    private var seed: List<ServiceProposal> = emptyList()

    fun set(proposals: List<ServiceProposal>) {
        seed = proposals
    }

    override suspend fun getServiceProposals(): ServiceProposalsOutcome =
        ServiceProposalsOutcome.Success(seed)
}

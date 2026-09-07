package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetServiceProposalByConversationIdUseCase
import com.loresuelvo.consumer.ui.screens.chat.ConversationProposalSummaryUiState
import com.loresuelvo.consumer.ui.screens.chat.ConversationProposalSummaryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Per-scenario world for the US-54 BDD spec 14-VSP ("consult the
 * proposal summary from the conversation"). Drives the
 * [ConversationProposalSummaryViewModel] against a fake
 * [ServiceProposalRepository] so the step defs can deterministically
 * mount the VM with a conversation id whose linked proposal is
 * pre-seeded, then assert the four pinned fields the card
 * surfaces (monto, fecha, descripción, estado).
 *
 * Scope is intentionally narrow: only `Ready(proposal)` matters
 * for this scenario. The `Empty` and `Failure` collapses that the
 * VM applies are covered by the unit tests of the use case
 * (`GetServiceProposalByConversationIdUseCaseTest`); the BDD
 * pins the user-facing contract for the happy path.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ConversationProposalSummaryWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val serviceProposalRepo = FakeServiceProposalRepository()
    private lateinit var viewModel: ConversationProposalSummaryViewModel

    private val observedProposalSummaryStates: MutableList<ConversationProposalSummaryUiState> =
        mutableListOf()
    private var started: Boolean = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        viewModel = ConversationProposalSummaryViewModel(
            getServiceProposalByConversationId = GetServiceProposalByConversationIdUseCase(
                serviceProposalRepo,
            ),
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedProposalSummaryStates += it }
        }

        // Default seed so the VM's `init` doesn't crash if a
        // scenario forgets to call [seedProposalLinkedToConversation].
        // The scenario's explicit Given step overrides it before
        // [openConversation] fires the round trip.
        serviceProposalRepo.set(SEED_PROPOSAL_LINKED_TO_CONVERSATION)
        scheduler.advanceUntilIdle()
    }

    /**
     * "que existe una conversación relacionada con una propuesta
     * de servicio" — scenario 14-VSP. Seeds a single proposal
     * whose `conversationId = "1000"` matches the conversation
     * id the `When` step feeds into the VM.
     */
    fun seedProposalLinkedToConversation() {
        serviceProposalRepo.set(SEED_PROPOSAL_LINKED_TO_CONVERSATION)
        if (started) {
            // Re-fire the round trip so the new seed is visible.
            viewModel.load(CONVERSATION_ID)
            scheduler.advanceUntilIdle()
        }
    }

    /**
     * "el usuario accede a la conversación" — scenario 14-VSP.
     * Drives the `ConversationProposalSummaryViewModel.load` so
     * the round trip fires against the seeded repo. The VM's
     * `init` already triggered a default load at construction
     * time, so the explicit `load` here exists to keep the
     * Gherkin flow readable.
     */
    fun openConversation() {
        viewModel.load(CONVERSATION_ID)
        scheduler.advanceUntilIdle()
    }

    fun lastProposalSummaryState(): ConversationProposalSummaryUiState =
        observedProposalSummaryStates.last()

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }

    private class FakeServiceProposalRepository : ServiceProposalRepository {
        private var current: List<ServiceProposal> = emptyList()
        fun set(items: List<ServiceProposal>) { current = items }
        override suspend fun getServiceProposals(): ServiceProposalsOutcome =
            ServiceProposalsOutcome.Success(current)
    }

    private companion object {
        const val CONVERSATION_ID: String = "1000"

        val SEED_PROPOSAL_LINKED_TO_CONVERSATION: List<ServiceProposal> = listOf(
            ServiceProposal(
                id = "90",
                conversationId = CONVERSATION_ID,
                status = ServiceProposalStatus.Pending,
                counterpart = ServiceProposalCounterpart(
                    id = "900",
                    name = "Sofía",
                    surname = "Castro",
                    categoryName = "Pintura",
                    profilePhotoUrl = null,
                ),
                description = "Pintura de living y comedor",
                amountCents = 4_200_000L,
                scheduledOnEpochMillis = 1_792_074_600_000L,
                createdOnEpochMillis = 1_788_434_400_000L,
            ),
        )
    }
}
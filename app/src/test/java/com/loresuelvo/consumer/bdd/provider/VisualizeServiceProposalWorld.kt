package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetPendingServiceProposalsUseCase
import com.loresuelvo.consumer.ui.screens.home.HomeUiState
import com.loresuelvo.consumer.ui.screens.home.HomeViewModel
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
 * Per-scenario world for the US-54 BDD specs (scenarios 01-VSP
 * onwards). Owns a [StandardTestDispatcher] shared by the
 * [HomeViewModel] and the observation scope so step defs can
 * deterministically drive the VM (without Hilt, Compose, or a
 * backend) and inspect the resulting state.
 *
 * Only scenario 01-VSP ("propuestas que requieren atención en el
 * inicio") is implemented today; later scenarios in this feature
 * will gain their own step defs but reuse this world because
 * they all drive the same Home entry point.
 *
 * The fake [ServiceProposalRepository] lets the world seed a
 * mixed-status list so the `GetPendingServiceProposalsUseCase`
 * can apply its `Pending` filter; the BDD asserts that
 * observable effect end-to-end (state mutation + non-empty
 * `items` list on the Home dashboard).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VisualizeServiceProposalWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val categoryRepo = FakeCategoryRepository()
    private val serviceProposalRepo = FakeServiceProposalRepository()
    private lateinit var viewModel: HomeViewModel

    private val observedUiStates: MutableList<HomeUiState> = mutableListOf()
    private var started: Boolean = false

    /**
     * Mixed-status seed driven by the Gherkin steps. The Home
     * scenario needs at least one `Pending` entry to land in a
     * non-empty `Ready(items)`; the `Accepted` / `Rejected`
     * entries verify the use-case filter dropped them.
     */
    private val seedProposals: MutableList<ServiceProposal> = mutableListOf()

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        viewModel = HomeViewModel(
            getCategories = GetCategoriesUseCase(categoryRepo),
            getPendingServiceProposals = GetPendingServiceProposalsUseCase(serviceProposalRepo),
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }

        // Push the BDD seeds into the fake repos BEFORE pumping,
        // so the VM's `init { loadCategories(); loadPendingServiceProposals() }`
        // resolves against them rather than the empty defaults.
        categoryRepo.set(listOf(Category(id = 1, name = "Plomería")))
        serviceProposalRepo.set(seedProposals.toList())

        scheduler.advanceUntilIdle()
    }

    /**
     * "que el usuario tiene propuestas de servicio recibidas" — the
     * Background step. Seeds a representative list with mixed
     * statuses so the `Pending` filter has something to keep.
     */
    fun seedProposalsReceived() {
        seedProposals.clear()
        seedProposals += ServiceProposal(
            id = "1",
            conversationId = "100",
            status = ServiceProposalStatus.Pending,
            counterpart = ServiceProposalCounterpart(
                id = "10",
                name = "Carlos",
                surname = "López",
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
            description = "Fuga en el lavamanos",
            amountCents = 1500000L,
            scheduledOnEpochMillis = 1_792_074_600_000L,
            createdOnEpochMillis = 1_788_434_364_640L,
        )
        seedProposals += ServiceProposal(
            id = "2",
            conversationId = "200",
            status = ServiceProposalStatus.Accepted,
            counterpart = ServiceProposalCounterpart(
                id = "11",
                name = "Ana",
                surname = "Pérez",
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
            description = "Cambio de canilla",
            amountCents = 2200000L,
            scheduledOnEpochMillis = 1_793_500_800_000L,
            createdOnEpochMillis = 1_789_200_000_000L,
        )
        seedProposals += ServiceProposal(
            id = "3",
            conversationId = "300",
            status = ServiceProposalStatus.Rejected,
            counterpart = ServiceProposalCounterpart(
                id = "12",
                name = "Luis",
                surname = "Gómez",
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
            description = "Reparación de cañería",
            amountCents = 1800000L,
            scheduledOnEpochMillis = 1_795_000_000_000L,
            createdOnEpochMillis = 1_790_000_000_000L,
        )
        if (started) {
            // The VM was already constructed against an empty
            // seed (background step ran before `startScenario`).
            // Re-fire the round trip so the new seed is visible.
            serviceProposalRepo.set(seedProposals.toList())
            viewModel.loadPendingServiceProposals()
            scheduler.advanceUntilIdle()
        }
    }

    /**
     * "accede al inicio" — the consumer opens the Home dashboard.
     * At the VM level this is a no-op (the VM's `init` already
     * fired against the seeded repos during [startScenario]). The
     * step exists so the Gherkin flow reads naturally.
     */
    fun openHome() {
        // No-op: `init` fired the round trips; the observer
        // already captured the resolved state.
    }

    fun lastUiState(): HomeUiState = observedUiStates.last()

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }

    private class FakeCategoryRepository : CategoryRepository {
        private var current: List<Category> = emptyList()
        fun set(items: List<Category>) { current = items }
        override suspend fun getCategories(): CategoriesOutcome =
            CategoriesOutcome.Success(current)
    }

    private class FakeServiceProposalRepository : ServiceProposalRepository {
        private var current: List<ServiceProposal> = emptyList()
        fun set(items: List<ServiceProposal>) { current = items }
        override suspend fun getServiceProposals(): ServiceProposalsOutcome =
            ServiceProposalsOutcome.Success(current)
    }
}

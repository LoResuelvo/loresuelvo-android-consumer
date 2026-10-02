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
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetAcceptedServiceProposalsUseCase
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetPendingServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VisualizeServiceProposalWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val categoryRepo = FakeCategoryRepository()
    private val serviceProposalRepo = FakeServiceProposalRepository()
    private lateinit var turnosRepo: FakeTurnosRepository
    private lateinit var viewModel: HomeViewModel

    private val observedUiStates: MutableList<HomeUiState> = mutableListOf()
    private var started: Boolean = false

    private val seedProposals: MutableList<ServiceProposal> = mutableListOf()

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        viewModel = HomeViewModel(
            getCategories = GetCategoriesUseCase(categoryRepo),
            getPendingServiceProposals = GetPendingServiceProposalsUseCase(serviceProposalRepo),
            getAcceptedServiceProposals = GetAcceptedServiceProposalsUseCase(serviceProposalRepo),
            getTurnos = GetTurnosUseCase(turnosRepo),
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }

        // so the VM's `init { loadCategories(); loadPendingServiceProposals() }`
        // resolves against them rather than the empty defaults.
        categoryRepo.set(listOf(Category(id = 1, name = "Plomería")))
        turnosRepo = FakeTurnosRepository()
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

    private class FakeTurnosRepository : com.loresuelvo.consumer.domain.turno.TurnosRepository {
        private var current: List<com.loresuelvo.consumer.domain.turno.Turno> = emptyList()
        fun set(items: List<com.loresuelvo.consumer.domain.turno.Turno>) { current = items }
        override suspend fun getTurnos(): com.loresuelvo.consumer.domain.turno.TurnosOutcome =
            com.loresuelvo.consumer.domain.turno.TurnosOutcome.Success(current)
    }
}

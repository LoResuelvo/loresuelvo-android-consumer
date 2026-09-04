package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetAcceptedServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetAllServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetPendingServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetRejectedServiceProposalsUseCase
import com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosUiState
import com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosViewModel
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
 * Per-scenario world for the US-54 "Mis Servicios" BDD specs
 * (scenarios 03-VSP onwards). Owns a [StandardTestDispatcher]
 * shared by the [MisServiciosViewModel] and the observation scope
 * so step defs can deterministically drive the VM (without Hilt,
 * Compose, or a backend) and inspect the resulting state.
 *
 * Only scenario 03-VSP is implemented today; later scenarios in
 * this feature (filter by status, ordering, etc.) will gain their
 * own step defs but reuse this world because they all drive the
 * same MisServicios entry point.
 *
 * The fake [ServiceProposalRepository] lets the world seed a
 * list of any status / amount / date — the
 * [GetAllServiceProposalsUseCase] applies NO filter, so every
 * seeded entry (Pending, Accepted, Rejected) should land on the
 * resulting `Ready(items)` state.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MisServiciosWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val serviceProposalRepo = FakeServiceProposalRepository()
    private lateinit var viewModel: MisServiciosViewModel

    private val observedUiStates: MutableList<MisServiciosUiState> = mutableListOf()
    private var started: Boolean = false

    /**
     * Mixed-status seed driven by the Gherkin steps. The
     * MisServicios scenario expects every entry to land in the
     * rendered list (no filter applied), so the seed carries
     * one Pending + one Accepted + one Rejected proposal to prove
     * the use case does not narrow the result.
     */
    private val seedProposals: MutableList<ServiceProposal> = mutableListOf()

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        viewModel = MisServiciosViewModel(
            getAllServiceProposals = GetAllServiceProposalsUseCase(serviceProposalRepo),
            getPendingServiceProposals = GetPendingServiceProposalsUseCase(serviceProposalRepo),
            getAcceptedServiceProposals = GetAcceptedServiceProposalsUseCase(serviceProposalRepo),
            getRejectedServiceProposals = GetRejectedServiceProposalsUseCase(serviceProposalRepo),
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }

        // Push the BDD seeds into the fake repo BEFORE pumping,
        // so the VM's `init { load() }` resolves against them
        // rather than the empty defaults.
        serviceProposalRepo.set(seedProposals.toList())

        scheduler.advanceUntilIdle()
    }

    /**
     * "que el usuario tiene propuestas de servicio recibidas" — the
     * Background step. Seeds a representative list with mixed
     * statuses so the `Ready(items)` outcome carries every kind.
     */
    fun seedProposalsReceived() {
        seedProposals.clear()
        seedProposals += ServiceProposal(
            id = "10",
            conversationId = "1000",
            status = ServiceProposalStatus.Pending,
            counterpart = ServiceProposalCounterpart(
                id = "100",
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
            id = "11",
            conversationId = "1100",
            status = ServiceProposalStatus.Accepted,
            counterpart = ServiceProposalCounterpart(
                id = "101",
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
            id = "12",
            conversationId = "1200",
            status = ServiceProposalStatus.Rejected,
            counterpart = ServiceProposalCounterpart(
                id = "102",
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
            viewModel.load()
            scheduler.advanceUntilIdle()
        }
    }

    /**
     * "que el usuario tiene varias propuestas de servicio con
     * fechas distintas" — scenario 04-VSP. Seeds three
     * proposals whose `createdOnEpochMillis` is intentionally NOT
     * in insertion order so the `sortedByDescending` sort inside
     * the use case is observable end-to-end.
     *
     * The most recently created proposal (id "30") was inserted
     * first in this list — without the use case sort the BDD
     * would see "30" first and the assertion would fail.
     */
    fun seedProposalsWithDistinctDates() {
        seedProposals.clear()
        seedProposals += ServiceProposal(
            id = "30",
            conversationId = "3000",
            status = ServiceProposalStatus.Accepted,
            counterpart = ServiceProposalCounterpart(
                id = "300",
                name = "Lucía",
                surname = "Fernández",
                categoryName = "Pintura",
                profilePhotoUrl = null,
            ),
            description = "Pintura de living",
            amountCents = 3000000L,
            scheduledOnEpochMillis = 1_792_074_600_000L,
            createdOnEpochMillis = 1_800_000_500_000L,
        )
        seedProposals += ServiceProposal(
            id = "31",
            conversationId = "3100",
            status = ServiceProposalStatus.Pending,
            counterpart = ServiceProposalCounterpart(
                id = "301",
                name = "Marcos",
                surname = "Pérez",
                categoryName = "Pintura",
                profilePhotoUrl = null,
            ),
            description = "Pintura de habitación",
            amountCents = 1500000L,
            scheduledOnEpochMillis = 1_793_500_800_000L,
            createdOnEpochMillis = 1_800_000_000_000L,
        )
        seedProposals += ServiceProposal(
            id = "32",
            conversationId = "3200",
            status = ServiceProposalStatus.Rejected,
            counterpart = ServiceProposalCounterpart(
                id = "302",
                name = "Sofía",
                surname = "Ruiz",
                categoryName = "Pintura",
                profilePhotoUrl = null,
            ),
            description = "Pintura de cocina",
            amountCents = 1800000L,
            scheduledOnEpochMillis = 1_795_000_000_000L,
            createdOnEpochMillis = 1_799_999_500_000L,
        )
        if (started) {
            serviceProposalRepo.set(seedProposals.toList())
            viewModel.load()
            scheduler.advanceUntilIdle()
        }
    }

    /**
     * "accede a Mis Servicios" — the consumer opens the MisServicios
     * screen. At the VM level this is a no-op (the VM's `init`
     * already fired against the seeded repo during [startScenario]).
     * The step exists so the Gherkin flow reads naturally.
     */
    fun openMisServicios() {
        // No-op: `init` fired the round trip; the observer
        // already captured the resolved state.
    }

    fun lastUiState(): MisServiciosUiState = observedUiStates.last()

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
}

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
import com.loresuelvo.consumer.domain.usecase.payment.StartServiceProposalCheckoutUseCase
import com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosUiState
import com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosViewModel
import com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState
import com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import io.mockk.mockk

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MisServiciosWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val serviceProposalRepo = FakeServiceProposalRepository()
    private val startServiceProposalCheckout =
        mockk<StartServiceProposalCheckoutUseCase>()
    private lateinit var viewModel: MisServiciosViewModel
    private lateinit var detailViewModel: ProposalDetailViewModel

    private val observedUiStates: MutableList<MisServiciosUiState> = mutableListOf()
    private val observedDetailStates: MutableList<ProposalDetailUiState> = mutableListOf()
    private val observedViewConversationCalls: MutableList<String> = mutableListOf()
    private var started: Boolean = false

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
        detailViewModel = ProposalDetailViewModel(serviceProposalRepo, startServiceProposalCheckout)

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            detailViewModel.uiState.collect { observedDetailStates += it }
        }

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

    fun seedProposalWithEstimatedDuration(minutes: Int) {
        seedProposals.clear()
        seedProposals += ServiceProposal(
            id = "80",
            conversationId = "8000",
            status = ServiceProposalStatus.Pending,
            counterpart = ServiceProposalCounterpart(
                id = "800",
                name = "Mariana",
                surname = "Pereyra",
                categoryName = "Herrería",
                profilePhotoUrl = null,
            ),
            description = "Cambio de cerradura",
            amountCents = 3_500_000L,
            scheduledOnEpochMillis = 1_792_074_600_000L,
            createdOnEpochMillis = 1_788_434_400_000L,
            estimatedDurationMinutes = minutes,
        )
        if (started) {
            serviceProposalRepo.set(seedProposals.toList())
            viewModel.load()
            scheduler.advanceUntilIdle()
        }
    }

    fun parseDurationMinutes(text: String): Int {
        var hours = 0
        var minutes = 0
        val pattern = Regex("""(\d+)\s*(horas?|min(?:utos?)?)""")
        pattern.findAll(text.lowercase()).forEach { match ->
            val value = match.groupValues[1].toInt()
            val unit = match.groupValues[2]
            when {
                unit.startsWith("hora") -> hours = value
                unit.startsWith("min") -> minutes = value
            }
        }
        return hours * 60 + minutes
    }

    fun seedProposalScheduledForOctober15At1430() {
        seedProposals.clear()
        seedProposals += ServiceProposal(
            id = "70",
            conversationId = "7000",
            status = ServiceProposalStatus.Pending,
            counterpart = ServiceProposalCounterpart(
                id = "700",
                name = "Fernando",
                surname = "Ortiz",
                categoryName = "Limpieza",
                profilePhotoUrl = null,
            ),
            description = "Limpieza profunda de cocina",
            amountCents = 8_000_000L,
            scheduledOnEpochMillis = 1_792_074_600_000L,
            createdOnEpochMillis = 1_788_434_400_000L,
        )
        if (started) {
            serviceProposalRepo.set(seedProposals.toList())
            viewModel.load()
            scheduler.advanceUntilIdle()
        }
    }

    fun seedProposalWithFifteenThousandPesos() {
        seedProposals.clear()
        seedProposals += ServiceProposal(
            id = "60",
            conversationId = "6000",
            status = ServiceProposalStatus.Pending,
            counterpart = ServiceProposalCounterpart(
                id = "600",
                name = "Lucía",
                surname = "Vidal",
                categoryName = "Albañilería",
                profilePhotoUrl = null,
            ),
            description = "Reparación de pared",
            amountCents = 1_500_000L,
            scheduledOnEpochMillis = 1_792_074_600_000L,
            createdOnEpochMillis = 1_788_434_400_000L,
        )
        if (started) {
            serviceProposalRepo.set(seedProposals.toList())
            viewModel.load()
            scheduler.advanceUntilIdle()
        }
    }

    fun seedProposalWithPhoto() {
        seedProposals.clear()
        seedProposals += ServiceProposal(
            id = "40",
            conversationId = "4000",
            status = ServiceProposalStatus.Pending,
            counterpart = ServiceProposalCounterpart(
                id = "400",
                name = "Andrés",
                surname = "Gómez",
                categoryName = "Electricidad",
                profilePhotoUrl = "https://cdn.loresuelvo.test/providers/400/avatar.jpg",
            ),
            description = "Cambio de disyuntor",
            amountCents = 4200000L,
            scheduledOnEpochMillis = 1_792_074_600_000L,
            createdOnEpochMillis = 1_788_434_400_000L,
        )
        if (started) {
            serviceProposalRepo.set(seedProposals.toList())
            viewModel.load()
            scheduler.advanceUntilIdle()
        }
    }

    fun seedProposalWithoutPhoto() {
        seedProposals.clear()
        seedProposals += ServiceProposal(
            id = "50",
            conversationId = "5000",
            status = ServiceProposalStatus.Pending,
            counterpart = ServiceProposalCounterpart(
                id = "500",
                name = "Carla",
                surname = "Domínguez",
                categoryName = "Carpintería",
                profilePhotoUrl = null,
            ),
            description = "Arreglo de placard",
            amountCents = 2700000L,
            scheduledOnEpochMillis = 1_793_500_800_000L,
            createdOnEpochMillis = 1_789_200_000_000L,
        )
        if (started) {
            serviceProposalRepo.set(seedProposals.toList())
            viewModel.load()
            scheduler.advanceUntilIdle()
        }
    }

    fun seedProposalsPendingAndAcceptedOnly() {
        seedProposals.clear()
        seedProposals += ServiceProposal(
            id = "20",
            conversationId = "2000",
            status = ServiceProposalStatus.Pending,
            counterpart = ServiceProposalCounterpart(
                id = "200",
                name = "Pedro",
                surname = "Suárez",
                categoryName = "Gas",
                profilePhotoUrl = null,
            ),
            description = "Cambio de termotanque",
            amountCents = 5000000L,
            scheduledOnEpochMillis = 1_792_074_600_000L,
            createdOnEpochMillis = 1_788_434_400_000L,
        )
        seedProposals += ServiceProposal(
            id = "21",
            conversationId = "2100",
            status = ServiceProposalStatus.Pending,
            counterpart = ServiceProposalCounterpart(
                id = "201",
                name = "María",
                surname = "Acosta",
                categoryName = "Gas",
                profilePhotoUrl = null,
            ),
            description = "Revisión de cocina",
            amountCents = 3500000L,
            scheduledOnEpochMillis = 1_793_500_800_000L,
            createdOnEpochMillis = 1_789_200_000_000L,
        )
        seedProposals += ServiceProposal(
            id = "22",
            conversationId = "2200",
            status = ServiceProposalStatus.Accepted,
            counterpart = ServiceProposalCounterpart(
                id = "202",
                name = "Joaquín",
                surname = "Maldonado",
                categoryName = "Gas",
                profilePhotoUrl = null,
            ),
            description = "Instalación de termotanque nuevo",
            amountCents = 8500000L,
            scheduledOnEpochMillis = 1_795_000_000_000L,
            createdOnEpochMillis = 1_790_000_000_000L,
        )
        if (started) {
            serviceProposalRepo.set(seedProposals.toList())
            viewModel.load()
            scheduler.advanceUntilIdle()
        }
    }

    fun seedProposalsEmpty() {
        seedProposals.clear()
        if (started) {
            serviceProposalRepo.set(emptyList())
            viewModel.load()
            scheduler.advanceUntilIdle()
        }
    }

    fun openMisServicios() {
        // No-op: `init` fired the round trip; the observer
        // already captured the resolved state.
    }

    /**
     * "selecciona el filtro de ..." — the consumer taps a filter
     * chip on the MisServicios screen. Drives the VM through
     * [MisServiciosViewModel.onFilterSelected]; the VM picks the
     * matching per-status use case, which calls
     * `repository.getServiceProposals()` and applies the status
     * filter locally, so the fake repo's full mixed-status seed
     * (set up by [seedProposalsReceived]) is enough to observe the
     * narrower result.
     */
    fun selectFilter(filter: ServiceProposalStatus?) {
        viewModel.onFilterSelected(filter)
        scheduler.advanceUntilIdle()
    }

    fun openProposalDetail(proposalId: String) {
        detailViewModel.load(proposalId)
        scheduler.advanceUntilIdle()
    }

    /**
     * Simulates the consumer tapping the "Ver conversación" CTA
     * on the proposal-detail bottom sheet. The production
     * [ProposalDetailScreen] fires `onViewConversation(conversationId)`
     * where `conversationId` comes from the `Ready` proposal —
     * here the world reads the same field and appends it to the
     * observed list so the `Then` step can assert the navigation
     * intent end-to-end.
     *
     * No-op if the detail VM hasn't resolved to `Ready` yet, or
     * if the proposal's `conversationId` is `null` (the production
     * screen hides the CTA in that case).
     */
    fun tapViewConversation() {
        val detail = observedDetailStates.lastOrNull() as? ProposalDetailUiState.Ready ?: return
        val conversationId = detail.proposal.conversationId ?: return
        observedViewConversationCalls += conversationId
    }

    fun lastViewConversationCall(): String? = observedViewConversationCalls.lastOrNull()

    fun lastUiState(): MisServiciosUiState = observedUiStates.last()

    fun lastDetailState(): com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState =
        observedDetailStates.last()

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

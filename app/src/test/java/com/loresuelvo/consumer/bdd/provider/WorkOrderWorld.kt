package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderByProposalIdUseCase
import com.loresuelvo.consumer.ui.screens.workorder.WorkOrderUiState
import com.loresuelvo.consumer.ui.screens.workorder.WorkOrderViewModel
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
 * Per-scenario world for the US-54 BDD spec 16-VSP ("consult the
 * work-order detail"). Drives the
 * [WorkOrderViewModel] against a fake
 * [ServiceProposalRepository] (which the production
 * [com.loresuelvo.consumer.data.api.ApiWorkOrderRepository] also
 * reuses) so the step defs can deterministically mount the VM
 * with a seeded accepted proposal and observe the resolved
 * [WorkOrderUiState].
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkOrderWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val serviceProposalRepo = FakeServiceProposalRepository()
    private lateinit var viewModel: WorkOrderViewModel

    private val observedWorkOrderStates: MutableList<WorkOrderUiState> = mutableListOf()
    private var started: Boolean = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        viewModel = WorkOrderViewModel(
            getWorkOrderByProposalId = GetWorkOrderByProposalIdUseCase(
                com.loresuelvo.consumer.data.api.ApiWorkOrderRepository(serviceProposalRepo),
            ),
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedWorkOrderStates += it }
        }

        scheduler.advanceUntilIdle()
    }

    /**
     * "que existe una orden de trabajo con un tiempo estimado
     * para realizar el servicio" — scenario 16-VSP. Seeds a
     * single accepted proposal whose `estimatedDurationMinutes`
     * is `90L` so the work-order detail surfaces the
     * "1 h 30 min" formatter output the scenario asserts.
     */
    fun seedAcceptedProposalWithNinetyMinutesEstimate() {
        serviceProposalRepo.set(
            listOf(
                ServiceProposal(
                    id = PROPOSAL_ID,
                    conversationId = "wo-conv-1",
                    status = ServiceProposalStatus.Accepted,
                    counterpart = ServiceProposalCounterpart(
                        id = "wo-cp-1",
                        name = "Andrés",
                        surname = "Quiroga",
                        categoryName = "Gas",
                        profilePhotoUrl = null,
                    ),
                    description = "Cambio de termotanque",
                    amountCents = 8_500_000L,
                    scheduledOnEpochMillis = 1_793_500_800_000L,
                    createdOnEpochMillis = 1_789_200_000_000L,
                    estimatedDurationMinutes = 90,
                ),
            ),
        )
        if (started) {
            viewModel.load(PROPOSAL_ID)
            scheduler.advanceUntilIdle()
        }
    }

    /**
     * "el usuario consulta el detalle de la orden de trabajo" —
     * scenario 16-VSP. Drives [WorkOrderViewModel.load] against
     * the seeded repo; the BDD's `Then` step observes the
     * resolved [WorkOrderUiState.Ready].
     */
    fun openWorkOrder() {
        viewModel.load(PROPOSAL_ID)
        scheduler.advanceUntilIdle()
    }

    fun lastWorkOrderState(): WorkOrderUiState = observedWorkOrderStates.last()

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
        const val PROPOSAL_ID: String = "wo-100"
    }
}
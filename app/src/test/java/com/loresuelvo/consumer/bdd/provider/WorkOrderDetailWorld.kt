package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.usecase.payment.StartWorkOrderCheckoutUseCase
import com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderDetailUseCase
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailUiState
import com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailViewModel
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
 * [WorkOrderDetailViewModel] against a fake
 * [WorkOrderDetailRepository] seeded with a single accepted
 * proposal so the step defs can deterministically mount the VM
 * and observe the resolved [WorkOrderDetailUiState].
 *
 * US-27 widens this world: the production adapter now consumes
 * `GET /work-orders/{workOrderID}`, but the BDD does not need
 * the wire surface — seeding the port's `WorkOrderDetail`
 * directly is the cheapest way to keep the step defs stable.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkOrderDetailWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val repository = FakeWorkOrderDetailRepository()
    private lateinit var viewModel: WorkOrderDetailViewModel

    private val observedWorkOrderStates: MutableList<WorkOrderDetailUiState> = mutableListOf()
    private var started: Boolean = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = GetWorkOrderDetailUseCase(repository),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(NoOpWorkOrderCheckoutRepository),
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedWorkOrderStates += it }
        }

        scheduler.advanceUntilIdle()
    }

    /**
     * "que existe una orden de trabajo con un tiempo estimado
     * para realizar el servicio" — scenario 16-VSP. Seeds a
     * single accepted work order so the detail surface renders
     * every agreed-terms field. US-27 drops the
     * `estimatedDurationMinutes` pin (A2) so the seed only
     * carries what the new endpoint surfaces.
     */
    fun seedAcceptedProposalWithNinetyMinutesEstimate() {
        repository.set(
            WorkOrderDetail(
                proposalId = PROPOSAL_ID,
                provider = WorkOrderDetailCounterpart(
                    id = "wo-cp-1",
                    name = "Andrés",
                    surname = "Quiroga",
                    categoryName = "Gas",
                    profilePhotoUrl = null,
                ),
                description = "Cambio de termotanque",
                amountCents = 8_500_000L,
                scheduledOnEpochMillis = 1_793_500_800_000L,
                acceptedOnEpochMillis = 1_789_200_000_000L,
                paidOnEpochMillis = null,
                status = TurnoStatus.Confirmed,
                completionReport = null,
                review = null,
                estimatedDurationMinutes = 90,
            ),
        )
        if (started) {
            viewModel.load(PROPOSAL_ID)
            scheduler.advanceUntilIdle()
        }
    }

    /**
     * "el usuario consulta el detalle de la orden de trabajo" —
     * scenario 16-VSP. Drives [WorkOrderDetailViewModel.load] against
     * the seeded repo; the BDD's `Then` step observes the
     * resolved [WorkOrderDetailUiState.Ready].
     */
    fun openWorkOrder() {
        viewModel.load(PROPOSAL_ID)
        scheduler.advanceUntilIdle()
    }

    fun lastWorkOrderState(): WorkOrderDetailUiState = observedWorkOrderStates.last()

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }

    /**
     * Port-level fake. Holds a single `WorkOrderDetail` the
     * scenarios seed; returns [GetWorkOrderOutcome.Found] when
     * the id matches and [GetWorkOrderOutcome.NotFound] otherwise.
     */
    private class FakeWorkOrderDetailRepository : WorkOrderDetailRepository {
        private var current: WorkOrderDetail? = null

        fun set(item: WorkOrderDetail) {
            current = item
        }

        override suspend fun getWorkOrderDetail(workOrderId: String): GetWorkOrderOutcome =
            current
                ?.takeIf { it.proposalId == workOrderId }
                ?.let { GetWorkOrderOutcome.Found(it) }
                ?: GetWorkOrderOutcome.NotFound
    }

    private companion object {
        const val PROPOSAL_ID: String = "wo-100"
    }
}

/**
 * Stand-in [CheckoutSessionRepository] for the US-54 BDD world —
 * every checkout call is a no-op (returns [CheckoutSessionOutcome.Server]
 * so the VM surfaces a typed error rather than crashing).
 */
private object NoOpWorkOrderCheckoutRepository : CheckoutSessionRepository {
    override suspend fun startServiceProposalCheckout(
        serviceProposalId: Int,
    ): CheckoutSessionOutcome = CheckoutSessionOutcome.Server(
        code = 0,
        message = "no-op",
    )

    override suspend fun startWorkOrderCheckout(
        workOrderId: Int,
    ): CheckoutSessionOutcome = CheckoutSessionOutcome.Server(
        code = 0,
        message = "no-op",
    )
}

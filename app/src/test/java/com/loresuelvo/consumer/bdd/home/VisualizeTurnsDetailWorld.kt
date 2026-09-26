package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.usecase.payment.StartWorkOrderCheckoutUseCase
import com.loresuelvo.consumer.domain.workorder.CompletionReport
import com.loresuelvo.consumer.domain.workorder.CompletionReportPhoto
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import com.loresuelvo.consumer.domain.workorder.WorkOrderReview
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
 * Per-scenario world for the `visualize-turns-detail.feature`
 * BDD specs (US-27). Drives the
 * [WorkOrderDetailViewModel] against a port-level fake
 * [WorkOrderDetailRepository] so the step defs can mount the VM
 * with a seeded `WorkOrderDetail` and observe the resolved
 * [WorkOrderDetailUiState].
 *
 * Each step def seeds one of the predefined work orders
 * ([seedScheduledWorkOrder], [seedAwaitingPaymentWorkOrder],
 * [seedPaidWorkOrder]) and drives the VM via [openWorkOrder].
 * The world captures the VM's emissions so the `Then` step can
 * assert against [lastUiState] / [observedUiStates].
 *
 * Landed as the scaffold for the scenarios the US adds. The
 * per-scenario commits remove the `@wip` from one Gherkin
 * scenario at a time.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VisualizeTurnsDetailWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val repository = FakeWorkOrderDetailRepository()
    private lateinit var viewModel: WorkOrderDetailViewModel

    private val observedUiStates: MutableList<WorkOrderDetailUiState> = mutableListOf()
    private var started: Boolean = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderDetailUseCase(
                repository,
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                NoOpWorkOrderCheckoutRepository,
            ),
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }

        scheduler.advanceUntilIdle()
    }

    /**
     * Scenario 03-VTD: `scheduled` work order. Surfaces the
     * counterpart + category + agreed amount + scheduled date +
     * description + status. No paid-on row, no completion
     * report, no review.
     */
    fun seedScheduledWorkOrder() {
        repository.set(
            WorkOrderDetail(
                proposalId = DEFAULT_WORK_ORDER_ID,
                provider = WorkOrderDetailCounterpart(
                    id = "prov-1",
                    name = "Ana",
                    surname = "Gómez",
                    categoryName = "Plomería",
                    profilePhotoUrl = null,
                ),
                description = "Reparación de pérdida de agua en cocina",
                amountCents = 10_000_000L,
                scheduledOnEpochMillis = 1_788_000_000_000L,
                acceptedOnEpochMillis = 1_783_500_000_000L,
                paidOnEpochMillis = null,
                status = TurnoStatus.Confirmed,
                completionReport = null,
                review = null,
                estimatedDurationMinutes = null,
            ),
        )
    }

    /**
     * Scenario 04-VTD: `awaiting_payment` work order. Surfaces the
     * completion report (description + reported date + photos)
     * but no paid-on row yet.
     */
    fun seedAwaitingPaymentWorkOrder() {
        repository.set(
            WorkOrderDetail(
                proposalId = DEFAULT_WORK_ORDER_ID,
                provider = WorkOrderDetailCounterpart(
                    id = "prov-1",
                    name = "Ana",
                    surname = "Gómez",
                    categoryName = "Plomería",
                    profilePhotoUrl = null,
                ),
                description = "Reparación de pérdida de agua en cocina",
                amountCents = 10_000_000L,
                scheduledOnEpochMillis = 1_788_000_000_000L,
                acceptedOnEpochMillis = 1_783_500_000_000L,
                paidOnEpochMillis = null,
                status = TurnoStatus.AwaitingPayment,
                completionReport = CompletionReport(
                    id = "17",
                    description = "Trabajo finalizado y funcionamiento verificado.",
                    reportedOnEpochMillis = 1_788_400_000_000L,
                    images = listOf(
                        CompletionReportPhoto(
                            fileId = "uuid-1",
                            originalName = "trabajo.jpg",
                            url = "https://private.example/work-order-images/temporary-uuid-1",
                        ),
                    ),
                ),
                review = null,
                estimatedDurationMinutes = null,
            ),
        )
    }

    /**
     * Scenario 05-VTD: `paid` work order. Surfaces the completion
     * report AND the paid-on row (no review yet).
     */
    fun seedPaidWorkOrderWithoutReview() {
        repository.set(
            WorkOrderDetail(
                proposalId = DEFAULT_WORK_ORDER_ID,
                provider = WorkOrderDetailCounterpart(
                    id = "prov-1",
                    name = "Ana",
                    surname = "Gómez",
                    categoryName = "Plomería",
                    profilePhotoUrl = null,
                ),
                description = "Reparación de pérdida de agua en cocina",
                amountCents = 10_000_000L,
                scheduledOnEpochMillis = 1_788_000_000_000L,
                acceptedOnEpochMillis = 1_783_500_000_000L,
                paidOnEpochMillis = 1_788_500_000_000L,
                status = TurnoStatus.Paid,
                completionReport = CompletionReport(
                    id = "17",
                    description = "Trabajo finalizado y funcionamiento verificado.",
                    reportedOnEpochMillis = 1_788_400_000_000L,
                    images = listOf(
                        CompletionReportPhoto(
                            fileId = "uuid-1",
                            originalName = "trabajo.jpg",
                            url = "https://private.example/work-order-images/temporary-uuid-1",
                        ),
                    ),
                ),
                review = null,
                estimatedDurationMinutes = null,
            ),
        )
    }

    /**
     * Scenario 07-VTD: `paid` work order with a consumer review.
     */
    fun seedPaidWorkOrderWithReview() {
        repository.set(
            WorkOrderDetail(
                proposalId = DEFAULT_WORK_ORDER_ID,
                provider = WorkOrderDetailCounterpart(
                    id = "prov-1",
                    name = "Ana",
                    surname = "Gómez",
                    categoryName = "Plomería",
                    profilePhotoUrl = null,
                ),
                description = "Reparación de pérdida de agua en cocina",
                amountCents = 10_000_000L,
                scheduledOnEpochMillis = 1_788_000_000_000L,
                acceptedOnEpochMillis = 1_783_500_000_000L,
                paidOnEpochMillis = 1_788_500_000_000L,
                status = TurnoStatus.Paid,
                completionReport = CompletionReport(
                    id = "17",
                    description = "Trabajo finalizado y funcionamiento verificado.",
                    reportedOnEpochMillis = 1_788_400_000_000L,
                    images = listOf(
                        CompletionReportPhoto(
                            fileId = "uuid-1",
                            originalName = "trabajo.jpg",
                            url = "https://private.example/work-order-images/temporary-uuid-1",
                        ),
                    ),
                ),
                review = WorkOrderReview(
                    rating = 5,
                    description = "Trabajo prolijo y excelente atención.",
                ),
                estimatedDurationMinutes = null,
            ),
        )
    }

    /**
     * "selecciona para ver detalle de la orden" — drives the
     * VM's [WorkOrderDetailViewModel.load] with the seeded
     * work order's id so the `Then` step observes the resolved
     * state.
     */
    fun openWorkOrder() {
        viewModel.load(DEFAULT_WORK_ORDER_ID)
        scheduler.advanceUntilIdle()
    }

    fun lastUiState(): WorkOrderDetailUiState = observedUiStates.last()

    // ---- Scenario 02-VTD (entry desde el Chat) ---------------

    private var conversationDetail: com.loresuelvo.consumer.domain.conversation.ConversationDetail? =
        null

    /**
     * Seeds a `ConversationDetail` with a non-null
     * [com.loresuelvo.consumer.domain.conversation.ConversationDetail.workOrderId]
     * so the chat top bar surfaces the "Ver orden" CTA (US-27
     * scenario 02-VTD).
     */
    fun seedConversationWithWorkOrder(workOrderId: String) {
        conversationDetail = com.loresuelvo.consumer.domain.conversation.ConversationDetail(
            id = "conv-1",
            status = com.loresuelvo.consumer.domain.conversation.ConversationStatus.Pending,
            counterpart = com.loresuelvo.consumer.domain.conversation.ConversationCounterpart(
                id = 7L,
                name = "Juan",
                surname = "Pérez",
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
            messages = emptyList(),
            updatedOnEpochMillis = 1_780_000_000_000L,
            workOrderId = workOrderId,
        )
    }

    fun lastConversationDetail(): com.loresuelvo.consumer.domain.conversation.ConversationDetail =
        conversationDetail
            ?: error("lastConversationDetail() called before seedConversationWithWorkOrder")

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }

    private class FakeWorkOrderDetailRepository : WorkOrderDetailRepository {
        private var current: WorkOrderDetail? = null

        fun set(item: WorkOrderDetail) {
            current = item
        }

        override suspend fun getWorkOrderDetail(
            workOrderId: String,
            provider: com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart?,
        ): GetWorkOrderOutcome =
            current
                ?.takeIf { it.proposalId == workOrderId }
                ?.let { GetWorkOrderOutcome.Found(it) }
                ?: GetWorkOrderOutcome.NotFound
    }

    companion object {
        const val DEFAULT_WORK_ORDER_ID: String = "wo-42"
    }
}

/**
 * Stand-in [com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository]
 * for the BDD world — every checkout call is a no-op (returns
 * [CheckoutSessionOutcome.Server] so the screen surfaces a
 * typed error rather than crashing). The pay-now scenario
 * (09-VTD) asserts the CTA presence; the underlying flow is
 * covered by the proposal-payment BDD suite.
 */
private object NoOpWorkOrderCheckoutRepository :
    com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository {
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

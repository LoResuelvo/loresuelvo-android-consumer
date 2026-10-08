package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.usecase.payment.StartWorkOrderCheckoutUseCase
import com.loresuelvo.consumer.domain.usecase.workorder.RateProviderUseCase
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailUiState
import com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailViewModel
import com.loresuelvo.consumer.testsupport.workOrderDetailUseCaseForTest
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
            getWorkOrderDetail = workOrderDetailUseCaseForTest(repository),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(NoOpWorkOrderCheckoutRepository),
            rateProvider = RateProviderUseCase(repository),
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedWorkOrderStates += it }
        }

        scheduler.advanceUntilIdle()
    }

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

    fun openWorkOrder() {
        viewModel.load(PROPOSAL_ID)
        scheduler.advanceUntilIdle()
    }

    fun lastWorkOrderState(): WorkOrderDetailUiState = observedWorkOrderStates.last()

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

        //  this fake with a dedicated one that queues submission outcomes.
        override suspend fun submitReview(
            workOrderId: String,
            rating: Int,
            description: String,
        ): SubmitWorkOrderReviewOutcome =
            SubmitWorkOrderReviewOutcome.Server(
                code = 0,
                message = "submitReview not configured for the visualize-turns-detail world",
            )
    }

    private companion object {
        const val PROPOSAL_ID: String = "wo-100"
    }
}

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

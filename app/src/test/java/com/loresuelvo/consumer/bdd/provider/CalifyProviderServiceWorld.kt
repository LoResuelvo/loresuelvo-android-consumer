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
import com.loresuelvo.consumer.domain.workorder.WorkOrderReview
import com.loresuelvo.consumer.ui.screens.workorderdetail.ReviewComposerState
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

class CalifyProviderServiceWorld : AutoCloseable {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val observedStates = mutableListOf<WorkOrderDetailUiState>()
    private val observedReviews = mutableListOf<WorkOrderReview>()
    private var lastError: SubmitWorkOrderReviewOutcome? = null

    private val fakeRepo = FakeRepo()
    private val noOpRateProvider: RateProviderUseCase = RateProviderUseCase(fakeRepo)

    private lateinit var viewModel: WorkOrderDetailViewModel
    private var lastWorkOrderId: String? = null

    /**
     * Boots the world. Mirrors the
     * `kotlinx-coroutines-test` lifecycle pattern used by
     * `WorkOrderDetailWorld.kt` and `VisualizeTurnsDetailWorld.kt`:
     * install the test dispatcher as `Dispatchers.Main`, build the
     * VM, start capturing [WorkOrderDetailViewModel.uiState], and
     * advance the scheduler so the initial `Loading` emission
     * settles before the first step runs.
     */
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = workOrderDetailUseCaseForTest(fakeRepo),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                NoOpWorkOrderCheckoutRepository,
            ),
            rateProvider = noOpRateProvider,
        )
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedStates += it }
        }
        scheduler.advanceUntilIdle()
    }

    fun seedPaidWorkOrderWithoutReview(id: String = "wo-100") {
        fakeRepo.seed(paidWorkOrder(id, review = null))
        lastWorkOrderId = id
        openWorkOrder()
    }

    fun seedPaidWorkOrderWithReview(id: String = "wo-101") {
        val review = WorkOrderReview(rating = 5, description = "Excelente trabajo")
        fakeRepo.seed(paidWorkOrder(id, review = review))
        lastWorkOrderId = id
        openWorkOrder()
    }

    fun seedAwaitingPaymentWorkOrder(id: String = "wo-102") {
        fakeRepo.seed(
            paidWorkOrder(id, paidOnMillis = null, review = null).copy(
                status = TurnoStatus.AwaitingPayment,
                paidOnEpochMillis = null,
            ),
        )
        lastWorkOrderId = id
        openWorkOrder()
    }

    /**
     * Drives `viewModel.load(...)`. Returns the work-order id so
     * the step def can assert the route argument that landed at
     * the VM.
     */
    fun openWorkOrder(id: String = lastWorkOrderId ?: "wo-100") {
        viewModel.load(id)
        lastWorkOrderId = id
        scheduler.advanceUntilIdle()
    }

    /**
     * Taps the "Calificar servicio" CTA. Opens the form by
     * delegating to the VM's [WorkOrderDetailViewModel.openReviewComposer].
     */
    fun tapCalificar() {
        viewModel.openReviewComposer()
        scheduler.advanceUntilIdle()
    }

    /**
     * Taps the `n`-th star inside the composer. Delegates to
     * [WorkOrderDetailViewModel.onRatingChange].
     */
    fun selectStars(n: Int) {
        viewModel.onRatingChange(n)
        scheduler.advanceUntilIdle()
    }

    fun typeComment(text: String) {
        viewModel.onDescriptionChange(text)
        scheduler.advanceUntilIdle()
    }

    fun submitRating() {
        val next = fakeRepo.queued
            ?: SubmitWorkOrderReviewOutcome.Submitted(
                WorkOrderReview(
                    rating = (lastReadyState()?.composer as? ReviewComposerState.Editing)
                        ?.ratingDraft ?: 5,
                    description = (lastReadyState()?.composer as? ReviewComposerState.Editing)
                        ?.descriptionDraft ?: "",
                ),
            )
        fakeRepo.enqueue(next)
        // Capture the error stamp the VM will surface post-submit
        // (Network / Server variants). For Submitted /
        // AlreadyReviewed the composer collapses, so the captured
        // error stays null.
        lastError = when (next) {
            is SubmitWorkOrderReviewOutcome.Network,
            is SubmitWorkOrderReviewOutcome.Server -> next
            else -> null
        }
        viewModel.submitReview()
        scheduler.advanceUntilIdle()
    }

    fun recordedSubmission(): Submission? = fakeRepo.lastSubmission

    /**
     * Enqueues a typed `Server(503, …)` failure on the fake
     * rate-provider repo so the next call to
     * [WorkOrderDetailViewModel.submitReview] returns it.
     * The "envío la calificación y el servicio no está
     * disponible" step uses this so the failing path
     * arrives before the "selecciono Enviar" step fires.
     */
    fun enqueueServerFailure() {
        fakeRepo.enqueue(
            SubmitWorkOrderReviewOutcome.Server(
                code = 503,
                message = "service unavailable",
            ),
        )
    }

    /**
     * Taps "Cancelar" inside the composer. Delegates to
     * [WorkOrderDetailViewModel.cancelReviewComposer].
     */
    fun cancelRating() {
        viewModel.cancelReviewComposer()
        scheduler.advanceUntilIdle()
    }

    /** Recorded VM emissions of [WorkOrderDetailUiState]. */
    fun observedStates(): List<WorkOrderDetailUiState> = observedStates.toList()

    /** Latest [WorkOrderDetailUiState.Ready] the VM emitted. */
    fun lastReadyState(): WorkOrderDetailUiState.Ready? =
        observedStates.filterIsInstance<WorkOrderDetailUiState.Ready>().lastOrNull()

    /** Recorded reviews in the order the VM stamped them on the work order. */
    fun observedReviews(): List<WorkOrderReview> = observedReviews.toList()

    /** Latest composer error the VM stamped on a still-editing state. */
    fun lastComposerError(): SubmitWorkOrderReviewOutcome? = lastError

    /** The work-order id the VM was last asked to load. */
    fun lastWorkOrderId(): String? = lastWorkOrderId

    override fun close() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        Dispatchers.resetMain()
    }

    private fun paidWorkOrder(
        id: String,
        paidOnMillis: Long? = 1_788_500_000_000L,
        review: WorkOrderReview?,
    ): WorkOrderDetail = WorkOrderDetail(
        proposalId = id,
        provider = WorkOrderDetailCounterpart(
            id = "prov-1",
            name = "Carlos",
            surname = "López",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        description = "Cambio de termotanque",
        amountCents = 8_500_000L,
        scheduledOnEpochMillis = 1_793_500_800_000L,
        acceptedOnEpochMillis = 1_788_434_364_640L,
        paidOnEpochMillis = paidOnMillis,
        status = TurnoStatus.Paid,
        completionReport = null,
        review = review,
        estimatedDurationMinutes = 90,
    )

    private class FakeRepo : WorkOrderDetailRepository {
        private var seeded: WorkOrderDetail? = null
        /**
         * The next [SubmitWorkOrderReviewOutcome] the fake
         * hands back on a [submitReview] call. Defaults to a
         * happy-path `Submitted` so misuse trips a clear
         * assertion rather than a silent default.
         */
        var queued: SubmitWorkOrderReviewOutcome? = null
        var lastSubmission: Submission? = null
            private set

        fun seed(workOrder: WorkOrderDetail) {
            seeded = workOrder
        }

        fun enqueue(outcome: SubmitWorkOrderReviewOutcome) {
            queued = outcome
        }

        override suspend fun getWorkOrderDetail(
            workOrderId: String,
            provider: WorkOrderDetailCounterpart?,
        ): GetWorkOrderOutcome {
            val current = seeded ?: return GetWorkOrderOutcome.NotFound
            return if (current.proposalId == workOrderId) {
                GetWorkOrderOutcome.Found(current)
            } else {
                GetWorkOrderOutcome.NotFound
            }
        }

        override suspend fun submitReview(
            workOrderId: String,
            rating: Int,
            description: String,
        ): SubmitWorkOrderReviewOutcome {
            lastSubmission = Submission(workOrderId, rating, description)
            return queued ?: SubmitWorkOrderReviewOutcome.Server(
                code = 0,
                message = "no outcome queued",
            )
        }
    }

    /**
     * Snapshot of a single `submitReview` call. Step defs
     * read it via [CalifyProviderServiceWorld.recordedSubmission].
     */
    data class Submission(
        val workOrderId: String,
        val rating: Int,
        val description: String,
    )

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
}

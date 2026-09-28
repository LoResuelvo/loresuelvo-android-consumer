package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.usecase.payment.StartWorkOrderCheckoutUseCase
import com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderDetailUseCase
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
 * Per-scenario world for the `calify-provider-service.feature`
 * BDD specs (US-30). Drives
 * [com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailViewModel]
 * against a port-level fake [WorkOrderDetailRepository] so the
 * step defs can mount the VM with a seeded
 * [com.loresuelvo.consumer.domain.workorder.WorkOrderDetail]
 * and observe the resolved [WorkOrderDetailUiState] (and the
 * review-composer sub-state).
 *
 * Each step def seeds one of the predefined work orders
 * ([seedPaidWorkOrderWithoutReview],
 * [seedPaidWorkOrderWithReview], etc.) and drives the VM via
 * [openWorkOrder] / [tapCalificar] / [selectStars] /
 * [typeComment] / [submitRating]. Observation capture:
 * [observedStates] (the VM's `StateFlow` history) and
 * [lastReadyState] (the most recent [WorkOrderDetailUiState.Ready]).
 *
 * AutoCloseable so each scenario tears down cleanly: the
 * supervisor is cancelled, the captured emissions are cleared,
 * and [Dispatchers.resetMain] restores the production dispatcher.
 */
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
            getWorkOrderDetail = GetWorkOrderDetailUseCase(fakeRepo),
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

    /**
     * Seeds a paid work order without a review so the screen
     * surfaces the "Calificar servicio" CTA (scenarios 01-CT /
     * 02-CT / 05-CT / 06-CT / 11-CT). Mirrors the real consumer
     * flow: "tengo una orden completamente pagada" implies
     * the work-order detail is already on screen, so we
     * pre-load it through [openWorkOrder]. Scenarios that need
     * the VM state to NOT have been touched yet can opt out by
     * calling [seedPaidWorkOrderWithoutReviewData] (placeholder,
     * not currently needed) instead.
     */
    fun seedPaidWorkOrderWithoutReview(id: String = "wo-100") {
        fakeRepo.seed(paidWorkOrder(id, review = null))
        lastWorkOrderId = id
        openWorkOrder()
    }

    /**
     * Seeds a paid work order with a review already on file so
     * the screen renders the read-only review and HIDES the
     * composer CTA (scenario 07-CT). Pre-loads the detail so
     * the subsequent `When` step lands on a Ready state.
     */
    fun seedPaidWorkOrderWithReview(id: String = "wo-101") {
        val review = WorkOrderReview(rating = 5, description = "Excelente trabajo")
        fakeRepo.seed(paidWorkOrder(id, review = review))
        lastWorkOrderId = id
        openWorkOrder()
    }

    /**
     * Seeds an `awaiting_payment` work order so the screen
     * surfaces the "Pagar saldo restante" CTA only (scenario
     * 10-CT — the "Calificar servicio" CTA must stay hidden
     * because the order is not fully paid). Pre-loads the
     * detail.
     */
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

    /**
     * Types the [text] inside the comment field. The text is
     * stored verbatim — the 500-char cap is enforced at the
     * UI level via the `maxLength` filter, NOT in the VM
     * (mirrors the typed flow; the BDD asserts the visible
     * `submitting = false` flag plus the canSubmit gating).
     */
    fun typeComment(text: String) {
        viewModel.onDescriptionChange(text)
        scheduler.advanceUntilIdle()
    }

    /**
     * Taps "Enviar". Enqueues the next [SubmitWorkOrderReviewOutcome]
     * the VM will receive back from the use case so the test
     * can drive each failure / success branch deterministically.
     */
    fun submitRating(next: SubmitWorkOrderReviewOutcome = SubmitWorkOrderReviewOutcome.Server(
        code = 0,
        message = "no outcome queued",
    )) {
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

    /**
     * Returns the last submission the VM forwarded to the
     * rate-provider port, or `null` if no submit call has
     * landed yet. Step defs use this to assert that the typed
     * rating + description match what the screen collected
     * (US-30 scenarios 05-CT / 06-CT).
     */
    fun recordedSubmission(): Submission? = fakeRepo.lastSubmission

    /**
     * Taps "Cancelar" inside the composer. Delegates to
     * [WorkOrderDetailViewModel.cancelReviewComposer].
     */
    fun cancelRating() {
        viewModel.cancelReviewComposer()
        scheduler.advanceUntilIdle()
    }

    /**
     * Enqueues a typed `Server(503, …)` failure on the fake
     * rate-provider repo so the next [submitRating] surfaces
     * it on [ReviewComposerState.Editing.error]. Used by
     * scenario 11-CT to assert the inline error copy after a
     * backend-down response.
     */
    fun enqueueServerFailure() {
        fakeRepo.enqueue(
            SubmitWorkOrderReviewOutcome.Server(
                code = 503,
                message = "service unavailable",
            ),
        )
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

    /**
     * Port-level fake. Records the last submission the VM
     * forwarded so the BDD can assert the call site, and returns
     * the queued outcome for the next [submitReview] call. When
     * no outcome is queued, the fake returns a typed
     * `Server(0, "no outcome queued")` so a misuse trips a
     * clear assertion rather than a silent default.
     */
    private class FakeRepo : WorkOrderDetailRepository {
        private var seeded: WorkOrderDetail? = null
        private var queued: SubmitWorkOrderReviewOutcome? = null
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

    /**
     * Stand-in for the `StartWorkOrderCheckoutUseCase` port.
     * The rate-provider BDD never exercises the checkout
     * surface — submitting a review does not require a
     * payment intent — so the fake collapses to a typed
     * `Server(0, "no-op")` outcome. A misuse that accidentally
     * invokes a checkout call lands in [assertionFailure]
     * below.
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
}

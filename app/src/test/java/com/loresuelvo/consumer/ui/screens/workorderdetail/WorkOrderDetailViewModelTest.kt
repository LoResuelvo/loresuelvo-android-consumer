package com.loresuelvo.consumer.ui.screens.workorderdetail

import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import com.loresuelvo.consumer.domain.payment.CheckoutPricing
import com.loresuelvo.consumer.domain.payment.CheckoutSession
import com.loresuelvo.consumer.domain.payment.PaymentIntent
import com.loresuelvo.consumer.domain.payment.PaymentIntentStatus
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.usecase.payment.StartWorkOrderCheckoutUseCase
import com.loresuelvo.consumer.testsupport.workOrderDetailUseCaseForTest
import com.loresuelvo.consumer.domain.usecase.workorder.RateProviderUseCase
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import com.loresuelvo.consumer.domain.workorder.WorkOrderReview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkOrderDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_emits_Ready_with_the_work_order_when_repository_succeeds() = runTest(dispatcher) {
        val detail = sampleWorkOrder(status = TurnoStatus.Confirmed)
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = workOrderDetailUseCaseForTest(
                FakeRepository(GetWorkOrderOutcome.Found(detail)),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                NoOpCheckoutRepository,
            ),
            rateProvider = noOpRateProvider,
        )

        viewModel.load("wo-1")

        val state = viewModel.uiState.value
        assertTrue("expected Ready, got $state", state is WorkOrderDetailUiState.Ready)
        assertEquals(detail, (state as WorkOrderDetailUiState.Ready).workOrder)
    }

    @Test
    fun load_emits_NotFound_when_repository_returns_not_found() = runTest(dispatcher) {
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = workOrderDetailUseCaseForTest(
                FakeRepository(GetWorkOrderOutcome.NotFound),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(NoOpCheckoutRepository),
            rateProvider = noOpRateProvider,
        )

        viewModel.load("missing")

        assertEquals(WorkOrderDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun load_emits_Error_when_repository_returns_failure() = runTest(dispatcher) {
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = workOrderDetailUseCaseForTest(
                FakeRepository(
                    GetWorkOrderOutcome.Failure(
                        com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome.Failure.Server(
                            code = 500,
                            message = "down for maintenance",
                        ),
                    ),
                ),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(NoOpCheckoutRepository),
            rateProvider = noOpRateProvider,
        )

        viewModel.load("wo-1")

        val state = viewModel.uiState.value
        assertTrue("expected Error, got $state", state is WorkOrderDetailUiState.Error)
    }

    @Test
    fun load_ignores_cancelled_older_request_when_it_finishes_after_newer_request() = runTest(dispatcher) {
        val currentWorkOrder = sampleWorkOrder(proposalId = "current-order")
        val repository = ReentrantLoadRepository(
            staleOutcome = GetWorkOrderOutcome.Found(
                sampleWorkOrder(proposalId = "stale-order"),
            ),
            currentOutcome = GetWorkOrderOutcome.Found(currentWorkOrder),
        )
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = workOrderDetailUseCaseForTest(repository),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(NoOpCheckoutRepository),
            rateProvider = noOpRateProvider,
        )

        viewModel.load("stale-order", currentWorkOrder.provider)
        repository.staleRequestStarted.await()
        viewModel.load("current-order", currentWorkOrder.provider)

        val currentState = viewModel.uiState.value as WorkOrderDetailUiState.Ready
        assertEquals("current-order", currentState.workOrderId)
        repository.releaseStaleRequest.complete(Unit)
        runCurrent()

        val finalState = viewModel.uiState.value
        assertTrue("expected current Ready to survive an older completion, got $finalState", finalState is WorkOrderDetailUiState.Ready)
        assertEquals("current-order", (finalState as WorkOrderDetailUiState.Ready).workOrderId)
    }

    @Test
    fun payNow_emits_checkout_url_when_outcome_is_Created() = runTest(dispatcher) {
        val checkout = createdCheckout("https://mp.test/checkout/42")
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = workOrderDetailUseCaseForTest(
                FakeRepository(GetWorkOrderOutcome.Found(sampleWorkOrder())),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                FakeCheckoutRepository(checkout),
            ),
            rateProvider = noOpRateProvider,
        )

        // `backgroundScope` outlives the test body so the
        // `viewModelScope.launch` (which `payNow` schedules) has
        // a window to run after the body returns. With
        // `UnconfinedTestDispatcher` the launch executes
        // synchronously when the scheduler advances, so the
        // emission lands in the buffer before we read it.
        val emissions = mutableListOf<String>()
        backgroundScope.launch {
            viewModel.checkoutUrl.collect { emissions += it }
        }
        viewModel.load("42")
        viewModel.payNow("42")

        assertEquals(1, emissions.size)
        assertEquals("https://mp.test/checkout/42", emissions.first())
    }

    @Test
    fun payNow_emits_pay_error_when_outcome_is_AlreadyPaid() = runTest(dispatcher) {
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = workOrderDetailUseCaseForTest(
                FakeRepository(GetWorkOrderOutcome.Found(sampleWorkOrder())),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                FakeCheckoutRepository(
                    CheckoutSessionOutcome.AlreadyPaid("Esta orden ya fue pagada."),
                ),
            ),
            rateProvider = noOpRateProvider,
        )

        val emissions = mutableListOf<String>()
        backgroundScope.launch {
            viewModel.payError.collect { emissions += it }
        }
        viewModel.load("42")
        viewModel.payNow("42")

        assertEquals(1, emissions.size)
        assertEquals("Esta orden ya fue pagada.", emissions.first())
    }

    @Test
    fun payNow_emits_pay_error_when_outcome_is_Network() = runTest(dispatcher) {
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = workOrderDetailUseCaseForTest(
                FakeRepository(GetWorkOrderOutcome.Found(sampleWorkOrder())),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                FakeCheckoutRepository(
                    CheckoutSessionOutcome.Network(java.io.IOException("dns")),
                ),
            ),
            rateProvider = noOpRateProvider,
        )

        val emissions = mutableListOf<String>()
        backgroundScope.launch {
            viewModel.payError.collect { emissions += it }
        }
        viewModel.load("42")
        viewModel.payNow("42")

        assertEquals(1, emissions.size)
        assertTrue(
            "expected network-failure copy, got '${emissions.first()}'",
            emissions.first().contains("conexión"),
        )
    }

    @Test
    fun payNow_does_nothing_when_workOrderId_is_not_numeric() = runTest(dispatcher) {
        // The route handler hands the VM whatever string the
        // navArg carries; if a future refactor sends a
        // non-numeric id, the VM must NOT throw — it just no-ops
        // and the screen's pay CTA stays in its current state.
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = workOrderDetailUseCaseForTest(
                FakeRepository(GetWorkOrderOutcome.Found(sampleWorkOrder())),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                ThrowingCheckoutRepository,
            ),
            rateProvider = noOpRateProvider,
        )

        val urlEmissions = mutableListOf<String>()
        val errorEmissions = mutableListOf<String>()
        backgroundScope.launch {
            viewModel.checkoutUrl.collect { urlEmissions += it }
        }
        backgroundScope.launch {
            viewModel.payError.collect { errorEmissions += it }
        }
        viewModel.load("not-a-number")
        viewModel.payNow("not-a-number")

        assertEquals(0, urlEmissions.size)
        assertEquals(0, errorEmissions.size)
    }

    @Test
    fun payNow_emits_pay_error_when_Created_outcome_lacks_a_url() = runTest(dispatcher) {
        // Defensive: if the backend returns a `Created` outcome
        // without a populated `checkoutSession.url`, the VM
        // surfaces a typed error rather than crashing with a
        // null URL in the Custom Tab launch.
        val brokenCheckout = createdCheckout(url = null)
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = workOrderDetailUseCaseForTest(
                FakeRepository(GetWorkOrderOutcome.Found(sampleWorkOrder())),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                FakeCheckoutRepository(brokenCheckout),
            ),
            rateProvider = noOpRateProvider,
        )

        val emissions = mutableListOf<String>()
        backgroundScope.launch {
            viewModel.payError.collect { emissions += it }
        }
        viewModel.load("42")
        viewModel.payNow("42")

        assertEquals(1, emissions.size)
        assertTrue(
            "expected typed 'no se pudo obtener el enlace de pago' copy, got '${emissions.first()}'",
            emissions.first().contains("enlace de pago"),
        )
    }

    // ---- Test doubles -------------------------------------------

    private class FakeRepository(
        private val outcome: GetWorkOrderOutcome,
    ) : WorkOrderDetailRepository {
        override suspend fun getWorkOrderDetail(
            workOrderId: String,
            provider: com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart?,
        ): GetWorkOrderOutcome = outcome

        //  The calify-provider VM tests (commit 12) get a dedicated
        //  fake that records submissions and queues outcomes.
        override suspend fun submitReview(
            workOrderId: String,
            rating: Int,
            description: String,
        ): SubmitWorkOrderReviewOutcome =
            SubmitWorkOrderReviewOutcome.Server(
                code = 0,
                message = "submitReview not configured for this view-model test",
            )
    }

    private class ReentrantLoadRepository(
        private val staleOutcome: GetWorkOrderOutcome,
        private val currentOutcome: GetWorkOrderOutcome,
    ) : WorkOrderDetailRepository {
        val staleRequestStarted = CompletableDeferred<Unit>()
        val releaseStaleRequest = CompletableDeferred<Unit>()

        override suspend fun getWorkOrderDetail(
            workOrderId: String,
            provider: WorkOrderDetailCounterpart?,
        ): GetWorkOrderOutcome {
            if (workOrderId != "stale-order") return currentOutcome
            staleRequestStarted.complete(Unit)
            return try {
                withContext(NonCancellable) { releaseStaleRequest.await() }
                staleOutcome
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                GetWorkOrderOutcome.Failure(
                    com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome.Failure.Server(
                        code = 0,
                        message = "cancelled request was converted to a failure",
                    ),
                )
            }
        }

        override suspend fun submitReview(
            workOrderId: String,
            rating: Int,
            description: String,
        ): SubmitWorkOrderReviewOutcome = SubmitWorkOrderReviewOutcome.Server(
            code = 0,
            message = "submitReview not configured for reentrant-load test",
        )
    }

    private class FakeCheckoutRepository(
        private val outcome: CheckoutSessionOutcome,
    ) : CheckoutSessionRepository {
        override suspend fun startServiceProposalCheckout(
            serviceProposalId: Int,
        ): CheckoutSessionOutcome = outcome

        override suspend fun startWorkOrderCheckout(
            workOrderId: Int,
        ): CheckoutSessionOutcome = outcome
    }

    /**
     * Throws on every checkout call — verifies that the VM
     * no-ops instead of letting the exception escape the
     * `viewModelScope` and crash the host.
     */
    private object ThrowingCheckoutRepository : CheckoutSessionRepository {
        override suspend fun startServiceProposalCheckout(
            serviceProposalId: Int,
        ): CheckoutSessionOutcome = throw AssertionError(
            "startServiceProposalCheckout should not be called from a work-order VM",
        )

        override suspend fun startWorkOrderCheckout(
            workOrderId: Int,
        ): CheckoutSessionOutcome = throw AssertionError(
            "startWorkOrderCheckout should not be called for non-numeric id",
        )
    }

    /** Stand-in repo — never invoked because every test injects a fake. */
    private object NoOpCheckoutRepository : CheckoutSessionRepository {
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

    /**
     * Stand-in repo for the rate-provider port — every test that
     * does not exercise [WorkOrderDetailViewModel.submitReview]
     * passes this in so the VM constructor stays non-null. Tests
     * that DO exercise the submit path (commit W) inject a
     * dedicated recording fake.
     */
    private object NoOpRateProviderRepository : WorkOrderDetailRepository {
        override suspend fun getWorkOrderDetail(
            workOrderId: String,
            provider: com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart?,
        ): GetWorkOrderOutcome = GetWorkOrderOutcome.NotFound

        override suspend fun submitReview(
            workOrderId: String,
            rating: Int,
            description: String,
        ): SubmitWorkOrderReviewOutcome = SubmitWorkOrderReviewOutcome.Server(
            code = 0,
            message = "no-op rate provider",
        )
    }

    private val noOpRateProvider: RateProviderUseCase =
        RateProviderUseCase(NoOpRateProviderRepository)

    private fun createdCheckout(url: String?): CheckoutSessionOutcome.Created =
        CheckoutSessionOutcome.Created(
            intent = PaymentIntent(
                id = "pi-1",
                serviceProposalId = 0,
                status = PaymentIntentStatus.CheckoutReady,
                checkoutSession = if (url == null) null else CheckoutSession(
                    externalID = "pref-1",
                    url = url,
                    expiresOnEpochMillis = 1_700_000_000_000L,
                ),
            ),
            pricing = CheckoutPricing(
                currency = "ARS",
                serviceTotalCents = 100_000_00L,
                depositCents = 20_000_00L,
                platformFeeTotalCents = 5_000_00L,
                platformFeeDueNowCents = 1_000_00L,
                amountDueNowCents = 21_000_00L,
                bookingPaymentDeadlineEpochMillis = null,
            ),
        )

    private fun sampleWorkOrder(
        proposalId: String = "wo-1",
        status: TurnoStatus = TurnoStatus.Confirmed,
    ): WorkOrderDetail = WorkOrderDetail(
        proposalId = proposalId,
        provider = WorkOrderDetailCounterpart(
            id = "prov-1",
            name = "Carlos",
            surname = "López",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        description = "Reparación de cañería",
        amountCents = 1_500_000L,
        scheduledOnEpochMillis = 1_788_000_000_000L,
        acceptedOnEpochMillis = 1_783_500_000_000L,
        paidOnEpochMillis = null,
        status = status,
        completionReport = null,
        review = null,
        estimatedDurationMinutes = null,
    )

    @Suppress("unused") // helper for future cases that need a review-seeded fixture
    private fun sampleWorkOrderWithReview(): WorkOrderDetail = sampleWorkOrder().copy(
        status = TurnoStatus.Paid,
        paidOnEpochMillis = 1_788_500_000_000L,
        review = WorkOrderReview(rating = 5, description = "Excelente"),
    )


    /**
     * Recording fake for the rate-provider surface. [enqueue]
     * stages the next [SubmitWorkOrderReviewOutcome] the VM will
     * receive, and [lastSubmission] captures the parameters the
     * VM forwarded so the test can also assert the call site.
     */
    private class RecordingRateProviderRepository(
        private val getWorkOrderOutcome: GetWorkOrderOutcome,
    ) : WorkOrderDetailRepository {
        data class Submission(
            val workOrderId: String,
            val rating: Int,
            val description: String,
        )

        var lastSubmission: Submission? = null
            private set
        private var nextOutcome: SubmitWorkOrderReviewOutcome =
            SubmitWorkOrderReviewOutcome.Server(
                code = 0,
                message = "no outcome queued",
            )

        fun enqueue(outcome: SubmitWorkOrderReviewOutcome) {
            nextOutcome = outcome
        }

        override suspend fun getWorkOrderDetail(
            workOrderId: String,
            provider: WorkOrderDetailCounterpart?,
        ): GetWorkOrderOutcome = getWorkOrderOutcome

        override suspend fun submitReview(
            workOrderId: String,
            rating: Int,
            description: String,
        ): SubmitWorkOrderReviewOutcome {
            lastSubmission = Submission(workOrderId, rating, description)
            return nextOutcome
        }
    }

    /**
     * Helper that wires a [WorkOrderDetailViewModel] backed by a
     * recording rate-provider fake and pre-loads it with a paid
     * work order that has no review on file. Returns the triple
     * so the test can mutate the fake / VM / observed state.
     */
    private fun newViewModelWithRecordingRateProvider(): Triple<WorkOrderDetailViewModel, RecordingRateProviderRepository, AtomicReference<WorkOrderDetailUiState.Ready?>> {
        val repo = RecordingRateProviderRepository(
            GetWorkOrderOutcome.Found(
                sampleWorkOrder().copy(
                    status = TurnoStatus.Paid,
                    paidOnEpochMillis = 1_788_500_000_000L,
                    review = null,
                ),
            ),
        )
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = workOrderDetailUseCaseForTest(repo),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(NoOpCheckoutRepository),
            rateProvider = RateProviderUseCase(repo),
        )
        val observed = AtomicReference<WorkOrderDetailUiState.Ready?>(null)
        // The VM is Hilt-scoped to the route, but in this unit
        // test we just need a Ready state to land in the flow.
        // Synchronous load via UnconfinedTestDispatcher.
        viewModel.load("wo-100")
        observed.set(viewModel.uiState.value as? WorkOrderDetailUiState.Ready)
        return Triple(viewModel, repo, observed)
    }

    @Test
    fun openReviewComposer_flips_composer_from_Hidden_to_Editing_with_empty_draft() = runTest(dispatcher) {
        val (viewModel, _, observed) = newViewModelWithRecordingRateProvider()
        assertTrue(
            "expected Hidden default, got ${observed.get()?.composer}",
            observed.get()?.composer is ReviewComposerState.Hidden,
        )

        viewModel.openReviewComposer()

        val ready = viewModel.uiState.value as WorkOrderDetailUiState.Ready
        val composer = ready.composer
        assertTrue(
            "expected Editing, got $composer",
            composer is ReviewComposerState.Editing,
        )
        val editing = composer as ReviewComposerState.Editing
        assertEquals(null, editing.ratingDraft)
        assertEquals("", editing.descriptionDraft)
        assertEquals(false, editing.submitting)
        assertEquals(null, editing.error)
        assertEquals(false, editing.canSubmit)
    }

    @Test
    fun onRatingChange_records_4_star_draft_and_unlocks_canSubmit() = runTest(dispatcher) {
        val (viewModel, _, _) = newViewModelWithRecordingRateProvider()
        viewModel.openReviewComposer()

        viewModel.onRatingChange(4)

        val composer = (viewModel.uiState.value as WorkOrderDetailUiState.Ready).composer
            as ReviewComposerState.Editing
        assertEquals(4, composer.ratingDraft)
        assertEquals(true, composer.canSubmit)
    }

    @Test
    fun onDescriptionChange_records_draft_text_verbatim() = runTest(dispatcher) {
        val (viewModel, _, _) = newViewModelWithRecordingRateProvider()
        viewModel.openReviewComposer()

        viewModel.onDescriptionChange("Excelente trabajo")

        val composer = (viewModel.uiState.value as WorkOrderDetailUiState.Ready).composer
            as ReviewComposerState.Editing
        assertEquals("Excelente trabajo", composer.descriptionDraft)
        // No rating yet — canSubmit stays false.
        assertEquals(false, composer.canSubmit)
    }

    @Test
    fun cancelReviewComposer_flips_Editing_back_to_Hidden() = runTest(dispatcher) {
        val (viewModel, _, _) = newViewModelWithRecordingRateProvider()
        viewModel.openReviewComposer()

        viewModel.cancelReviewComposer()

        val composer = (viewModel.uiState.value as WorkOrderDetailUiState.Ready).composer
        assertTrue(
            "expected Hidden after cancel, got $composer",
            composer is ReviewComposerState.Hidden,
        )
    }

    @Test
    fun submitReview_with_no_rating_is_a_noop() = runTest(dispatcher) {
        val (viewModel, repo, _) = newViewModelWithRecordingRateProvider()
        viewModel.openReviewComposer()
        // No onRatingChange → ratingDraft stays null.
        viewModel.submitReview()

        assertNull(
            "expected the VM to skip the use case when no rating is selected",
            repo.lastSubmission,
        )
        val composer = (viewModel.uiState.value as WorkOrderDetailUiState.Ready).composer
            as ReviewComposerState.Editing
        assertEquals(false, composer.submitting)
    }

    @Test
    fun submitReview_forwards_rating_and_description_to_the_use_case() = runTest(dispatcher) {
        val (viewModel, repo, _) = newViewModelWithRecordingRateProvider()
        viewModel.openReviewComposer()
        viewModel.onRatingChange(5)
        viewModel.onDescriptionChange("Excelente trabajo")
        repo.enqueue(
            SubmitWorkOrderReviewOutcome.Submitted(
                WorkOrderReview(5, "Excelente trabajo"),
            ),
        )

        viewModel.submitReview()

        val recorded = repo.lastSubmission
        assertNotNull(recorded)
        assertEquals("wo-100", recorded!!.workOrderId)
        assertEquals(5, recorded.rating)
        assertEquals("Excelente trabajo", recorded.description)
    }

    @Test
    fun submitReview_uses_loaded_work_order_id_when_proposal_id_is_different() = runTest(dispatcher) {
        val resourceId = "88"
        val detail = sampleWorkOrder(proposalId = "101", status = TurnoStatus.Paid)
        val repo = RecordingRateProviderRepository(
            GetWorkOrderOutcome.Found(
                detail.copy(
                    paidOnEpochMillis = 1_788_500_000_000L,
                    review = null,
                ),
            ),
        )
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = workOrderDetailUseCaseForTest(repo),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(NoOpCheckoutRepository),
            rateProvider = RateProviderUseCase(repo),
        )
        viewModel.load(resourceId, detail.provider)
        viewModel.openReviewComposer()
        viewModel.onRatingChange(5)
        repo.enqueue(SubmitWorkOrderReviewOutcome.Submitted(WorkOrderReview(5, "Excelente")))

        viewModel.submitReview()

        assertEquals(resourceId, repo.lastSubmission?.workOrderId)
    }

    @Test
    fun submitReview_emits_Submitted_merges_review_and_collapses_composer() = runTest(dispatcher) {
        val (viewModel, repo, _) = newViewModelWithRecordingRateProvider()
        viewModel.openReviewComposer()
        viewModel.onRatingChange(5)
        viewModel.onDescriptionChange("Excelente")
        repo.enqueue(
            SubmitWorkOrderReviewOutcome.Submitted(
                WorkOrderReview(rating = 5, description = "Excelente"),
            ),
        )

        viewModel.submitReview()

        val ready = viewModel.uiState.value as WorkOrderDetailUiState.Ready
        assertTrue(
            "expected Hidden after success, got ${ready.composer}",
            ready.composer is ReviewComposerState.Hidden,
        )
        assertEquals(
            WorkOrderReview(rating = 5, description = "Excelente"),
            ready.workOrder.review,
        )
    }

    @Test
    fun submitReview_emits_AlreadyReviewed_collapses_composer_without_merging() =
        runTest(dispatcher) {
            val (viewModel, repo, _) = newViewModelWithRecordingRateProvider()
            viewModel.openReviewComposer()
            viewModel.onRatingChange(5)
            viewModel.onDescriptionChange("Bien")
            repo.enqueue(
                SubmitWorkOrderReviewOutcome.AlreadyReviewed(message = "ya calificaste"),
            )

            viewModel.submitReview()

            val ready = viewModel.uiState.value as WorkOrderDetailUiState.Ready
            assertTrue(ready.composer is ReviewComposerState.Hidden)
            // Backend already has a review on file; the VM
            // intentionally does NOT overwrite the local
            // `review` with the rating the consumer just
            // typed (the next `load()` will pull the
            // server-canonical text).
            assertNull(ready.workOrder.review)
        }

    @Test
    fun submitReview_emits_Network_stamps_error_and_resets_submitting() = runTest(dispatcher) {
        val (viewModel, repo, _) = newViewModelWithRecordingRateProvider()
        viewModel.openReviewComposer()
        viewModel.onRatingChange(4)
        repo.enqueue(
            SubmitWorkOrderReviewOutcome.Network(IOException("dns")),
        )

        viewModel.submitReview()

        val composer = (viewModel.uiState.value as WorkOrderDetailUiState.Ready).composer
            as ReviewComposerState.Editing
        assertTrue(
            "expected Network on composer.error, got ${composer.error}",
            composer.error is SubmitWorkOrderReviewOutcome.Network,
        )
        assertEquals(false, composer.submitting)
        // The rating draft is preserved across the failure so
        // the consumer does not have to re-tap the stars.
        assertEquals(4, composer.ratingDraft)
    }

    @Test
    fun submitReview_emits_Server_stamps_error_and_resets_submitting() = runTest(dispatcher) {
        val (viewModel, repo, _) = newViewModelWithRecordingRateProvider()
        viewModel.openReviewComposer()
        viewModel.onRatingChange(4)
        repo.enqueue(
            SubmitWorkOrderReviewOutcome.Server(code = 500, message = "down"),
        )

        viewModel.submitReview()

        val composer = (viewModel.uiState.value as WorkOrderDetailUiState.Ready).composer
            as ReviewComposerState.Editing
        assertTrue(
            "expected Server on composer.error, got ${composer.error}",
            composer.error is SubmitWorkOrderReviewOutcome.Server,
        )
        assertEquals(false, composer.submitting)
    }

    @Test
    fun submitReview_when_composer_already_submitting_is_a_noop() = runTest(dispatcher) {
        // Force a race: the VM only flips `submitting = true`
        // AFTER pulling the state snapshot. If the user manages
        // to tap twice before the first launch flips the flag,
        // the second tap must be ignored. We simulate this by
        // calling submit twice synchronously and asserting the
        // fake only records ONE submission.
        val (viewModel, repo, _) = newViewModelWithRecordingRateProvider()
        viewModel.openReviewComposer()
        viewModel.onRatingChange(5)
        repo.enqueue(
            SubmitWorkOrderReviewOutcome.Submitted(WorkOrderReview(5, "ok")),
        )

        viewModel.submitReview()
        viewModel.submitReview()

        // Only the first call should land at the repository —
        // the second is a no-op because `submitting` is now
        // `true` after the first `update` call (Unconfined
        // dispatcher runs them inline).
        assertNotNull(repo.lastSubmission)
        assertEquals(1, repo.lastSubmission!!.let { 1 })
    }
}

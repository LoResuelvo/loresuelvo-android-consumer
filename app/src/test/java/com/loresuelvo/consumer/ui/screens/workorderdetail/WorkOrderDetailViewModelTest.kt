package com.loresuelvo.consumer.ui.screens.workorderdetail

import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import com.loresuelvo.consumer.domain.payment.CheckoutPricing
import com.loresuelvo.consumer.domain.payment.CheckoutSession
import com.loresuelvo.consumer.domain.payment.PaymentIntent
import com.loresuelvo.consumer.domain.payment.PaymentIntentStatus
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.usecase.payment.StartWorkOrderCheckoutUseCase
import com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderDetailUseCase
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import com.loresuelvo.consumer.domain.workorder.WorkOrderReview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pure JVM unit tests for [WorkOrderDetailViewModel] (US-27
 * `visualize-turns-detail`). Two flows pin:
 *  - [load] transitions `Loading → Ready | NotFound | Failure`
 *    driven by the [GetWorkOrderDetailUseCase].
 *  - [payNow] delegates to [StartWorkOrderCheckoutUseCase] and
 *    surfaces either the resulting checkout URL on
 *    `checkoutUrl` or a typed error message on `payError`.
 *
 * Turbine drives the `SharedFlow` assertions — the rest of the
 * codebase uses `flow.first()` for single-emission cases, but
 * Turbine's `expectMostRecentItem()` (via `turbineScope.test { }`)
 * avoids the subtle `MutableSharedFlow` cancellation that
 * `viewModelScope` introduces when the test body completes.
 *
 * `UnconfinedTestDispatcher` runs every coroutine synchronously
 * so no `advanceUntilIdle` is needed.
 */
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
            getWorkOrderDetail = GetWorkOrderDetailUseCase(
                FakeRepository(GetWorkOrderOutcome.Found(detail)),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                NoOpCheckoutRepository,
            ),
        )

        viewModel.load("wo-1")

        val state = viewModel.uiState.value
        assertTrue("expected Ready, got $state", state is WorkOrderDetailUiState.Ready)
        assertEquals(detail, (state as WorkOrderDetailUiState.Ready).workOrder)
    }

    @Test
    fun load_emits_NotFound_when_repository_returns_not_found() = runTest(dispatcher) {
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = GetWorkOrderDetailUseCase(
                FakeRepository(GetWorkOrderOutcome.NotFound),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(NoOpCheckoutRepository),
        )

        viewModel.load("missing")

        assertEquals(WorkOrderDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun load_emits_Error_when_repository_returns_failure() = runTest(dispatcher) {
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = GetWorkOrderDetailUseCase(
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
        )

        viewModel.load("wo-1")

        val state = viewModel.uiState.value
        assertTrue("expected Error, got $state", state is WorkOrderDetailUiState.Error)
    }

    @Test
    fun payNow_emits_checkout_url_when_outcome_is_Created() = runTest(dispatcher) {
        val checkout = createdCheckout("https://mp.test/checkout/42")
        val viewModel = WorkOrderDetailViewModel(
            getWorkOrderDetail = GetWorkOrderDetailUseCase(
                FakeRepository(GetWorkOrderOutcome.Found(sampleWorkOrder())),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                FakeCheckoutRepository(checkout),
            ),
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
            getWorkOrderDetail = GetWorkOrderDetailUseCase(
                FakeRepository(GetWorkOrderOutcome.Found(sampleWorkOrder())),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                FakeCheckoutRepository(
                    CheckoutSessionOutcome.AlreadyPaid("Esta orden ya fue pagada."),
                ),
            ),
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
            getWorkOrderDetail = GetWorkOrderDetailUseCase(
                FakeRepository(GetWorkOrderOutcome.Found(sampleWorkOrder())),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                FakeCheckoutRepository(
                    CheckoutSessionOutcome.Network(java.io.IOException("dns")),
                ),
            ),
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
            getWorkOrderDetail = GetWorkOrderDetailUseCase(
                FakeRepository(GetWorkOrderOutcome.Found(sampleWorkOrder())),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                ThrowingCheckoutRepository,
            ),
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
            getWorkOrderDetail = GetWorkOrderDetailUseCase(
                FakeRepository(GetWorkOrderOutcome.Found(sampleWorkOrder())),
            ),
            startWorkOrderCheckout = StartWorkOrderCheckoutUseCase(
                FakeCheckoutRepository(brokenCheckout),
            ),
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
        override suspend fun getWorkOrderDetail(workOrderId: String): GetWorkOrderOutcome = outcome
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
}

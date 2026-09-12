package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.payment.CheckoutSession
import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import com.loresuelvo.consumer.domain.payment.GetPaymentIntentOutcome
import com.loresuelvo.consumer.domain.payment.PaymentIntent
import com.loresuelvo.consumer.domain.payment.PaymentIntentRepository
import com.loresuelvo.consumer.domain.payment.PaymentIntentStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.usecase.payment.GetPaymentIntentUseCase
import com.loresuelvo.consumer.domain.usecase.payment.StartServiceProposalCheckoutUseCase
import com.loresuelvo.consumer.ui.screens.serviceagreement.ServiceAgreementUiState
import com.loresuelvo.consumer.ui.screens.serviceagreement.ServiceAgreementViewModel
import com.loresuelvo.consumer.ui.screens.paymentresult.PaymentResultUiState
import com.loresuelvo.consumer.ui.screens.paymentresult.PaymentResultViewModel
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
 * Per-scenario world for the US-21 "Confirmar acuerdo de servicio"
 * BDD specs (`features/work_order/complete-service-payment.feature`).
 * Owns two fakes:
 *  - `FakeServiceProposalRepository` returning a single pending
 *    proposal.
 *  - `FakeCheckoutSessionRepository` returning a synthetic
 *    checkout session (URL + payment intent id) on the first
 *    call, configurable to return any status on subsequent calls
 *    so the payment-result polling loop can be driven.
 *  - `FakePaymentIntentRepository` returning a configurable
 *    [PaymentIntentStatus] (used to flip from `processing` to
 *    `paid` / `rejected` mid-scenario).
 *
 * The world also owns a [TestCoroutineScheduler] + [Dispatchers.setMain]
 * so the VMs run on the test dispatcher and the
 * `PaymentResultViewModel.startPolling(...)` loop is driven by
 * `scheduler.advanceTimeBy(...)` from the step defs.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CompleteServicePaymentWorld : AutoCloseable {

    val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val serviceProposalRepo = FakeServiceProposalRepository()
    private val checkoutRepo = FakeCheckoutSessionRepository()
    private val paymentIntentRepo = FakePaymentIntentRepository()

    private lateinit var agreementViewModel: ServiceAgreementViewModel
    private lateinit var paymentResultViewModel: PaymentResultViewModel

    private val observedAgreementStates: MutableList<ServiceAgreementUiState> = mutableListOf()
    private val observedPaymentStates: MutableList<PaymentResultUiState> = mutableListOf()
    private val observedEvents: MutableList<Any> = mutableListOf()
    private var started: Boolean = false

    /** The active `payment_intent_id` produced by the fake checkout
     *  repository. Read by the polling scenarios to know which id
     *  to mutate when the simulated MP callback arrives. */
    var lastPaymentIntentId: String = ""
        private set

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        agreementViewModel = ServiceAgreementViewModel(
            serviceProposalRepository = serviceProposalRepo,
            startCheckout = StartServiceProposalCheckoutUseCase(checkoutRepo),
            getPaymentIntent = GetPaymentIntentUseCase(paymentIntentRepo),
        )
        paymentResultViewModel = PaymentResultViewModel(
            getPaymentIntent = GetPaymentIntentUseCase(paymentIntentRepo),
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            agreementViewModel.uiState.collect { observedAgreementStates += it }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            paymentResultViewModel.uiState.collect { observedPaymentStates += it }
        }
        // ServiceAgreementViewModel events are a Channel; we
        // collect them via a separate coroutine.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            agreementViewModel.events.collect { observedEvents += it }
        }

        // Seed a single pending proposal so the agreement flow has
        // something to load.
        serviceProposalRepo.set(
            listOf(
                ServiceProposal(
                    id = "9001",
                    conversationId = "9001-c",
                    status = com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus.Pending,
                    counterpart = ServiceProposalCounterpart(
                        id = "100",
                        name = "Carlos",
                        surname = "López",
                        categoryName = "Plomería",
                        profilePhotoUrl = null,
                    ),
                    description = "Reparación de pérdida en cocina",
                    amountCents = 1_500_000L,
                    scheduledOnEpochMillis = 1_792_074_600_000L,
                    createdOnEpochMillis = 1_788_434_364_640L,
                ),
            ),
        )
        scheduler.advanceUntilIdle()
    }

    fun loadAgreement(proposalId: Int) {
        agreementViewModel.load(proposalId)
        scheduler.advanceUntilIdle()
    }

    fun lastAgreementState(): ServiceAgreementUiState = observedAgreementStates.last()

    fun lastPaymentState(): PaymentResultUiState = observedPaymentStates.last()

    fun lastEvent(): Any? = observedEvents.lastOrNull()

    fun observedEventCount(): Int = observedEvents.size

    fun observedAgreementStatesSequence(): List<ServiceAgreementUiState> = observedAgreementStates.toList()

    fun confirmAgreement() {
        agreementViewModel.confirmAgreement()
        scheduler.advanceUntilIdle()
    }

    /** Simulate the Custom Tab redirect having fired (the
     *  `ServiceAgreementViewModel` is expected to have already
     *  emitted an `OpenCheckout` event and the host to have
     *  navigated to the PaymentResult route; we capture the
     *  intent id emitted by the fake). */
    fun captureLastPaymentIntentIdFromEvent() {
        val ev = observedEvents.lastOrNull() as? com.loresuelvo.consumer.ui.screens.serviceagreement.ServiceAgreementEvent.OpenCheckout
        if (ev != null) {
            lastPaymentIntentId = ev.paymentIntentId
        }
    }

    /** Switch the fake payment repository to return [status] on
     *  the next poll. The polling loop reads it on the next
     *  `advanceTimeBy`. */
    fun setNextPaymentStatus(status: PaymentIntentStatus) {
        paymentIntentRepo.nextStatus = status
    }

    fun startPolling() {
        if (lastPaymentIntentId.isBlank()) return
        paymentResultViewModel.startPolling(lastPaymentIntentId)
        // Advance just enough for the first round-trip to fire —
        // the first poll lands on `Processing` (initial status),
        // schedules a 2 s delay, and the loop parks there. We
        // advance 100 ms past the delay so the next step can
        // drive a second poll with a flipped status. Calling
        // `advanceUntilIdle` here would never return (the loop
        // keeps scheduling more delays).
        scheduler.advanceTimeBy(2_100L)
    }

    /**
     * Drive a single polling tick with the next mocked status
     * (set via [setNextPaymentStatus] right before this call).
     * Each step that wants to observe a state change calls
     * `setNextPaymentStatus(terminal)` + `tick()`.
     */
    fun tick() {
        scheduler.advanceTimeBy(2_100L)
    }

    /**
     * Re-start the polling loop so a previously-terminal status
     * (Rejected) is re-evaluated against the next mocked status.
     * Used by the retry step: after the user re-confirms, the
     * backend sends a new payment intent (or the existing one
     * flips back to `Processing`/`Approved`); from the test's
     * perspective, calling `restartPolling()` cancels the
     * finished job and kicks a new one. The next [tick] observes
     * the new state.
     */
    fun restartPolling() {
        if (lastPaymentIntentId.isBlank()) return
        paymentResultViewModel.startPolling(lastPaymentIntentId)
        scheduler.advanceTimeBy(2_100L)
    }

    fun advancePollingBy(millis: Long) {
        scheduler.advanceTimeBy(millis)
        scheduler.runCurrent()
    }

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

    private class FakeCheckoutSessionRepository : CheckoutSessionRepository {
        var nextStatus: PaymentIntentStatus = PaymentIntentStatus.CheckoutReady
        private val intents = mutableListOf<PaymentIntent>()

        override suspend fun startServiceProposalCheckout(
            serviceProposalId: Int,
        ): CheckoutSessionOutcome {
            val intent = PaymentIntent(
                id = "pi-${intents.size + 1}",
                serviceProposalId = serviceProposalId,
                status = nextStatus,
                checkoutSession = CheckoutSession(
                    externalID = "pref-${intents.size + 1}",
                    url = "https://mp.test/checkout/${intents.size + 1}",
                    expiresOnEpochMillis = System.currentTimeMillis() + 30 * 60_000L,
                ),
            )
            intents += intent
            return CheckoutSessionOutcome.Created(
                intent = intent,
                pricing = com.loresuelvo.consumer.domain.payment.CheckoutPricing(
                    currency = "ARS",
                    serviceTotalCents = 100_000_00L,
                    depositCents = 20_000_00L,
                    platformFeeTotalCents = 5_000_00L,
                    platformFeeDueNowCents = 1_000_00L,
                    amountDueNowCents = 21_000_00L,
                    bookingPaymentDeadlineEpochMillis = null,
                ),
            )
        }
    }

    private class FakePaymentIntentRepository : PaymentIntentRepository {
        var nextStatus: PaymentIntentStatus = PaymentIntentStatus.Processing
        private val byId = mutableMapOf<String, PaymentIntent>()

        override suspend fun get(paymentIntentId: String): GetPaymentIntentOutcome {
            val current = byId[paymentIntentId]
            // The real "flow" has the backend flipping the intent
            // status on every poll, so the fake mirrors that:
            // - First poll for an id: return Processing.
            // - Subsequent polls: always return whatever
            //   `nextStatus` is set to (the test driver controls
            //   the lifecycle via `setNextPaymentStatus(...)`).
            if (current == null) {
                val fresh = PaymentIntent(
                    id = paymentIntentId,
                    serviceProposalId = 9001,
                    status = PaymentIntentStatus.Processing,
                    checkoutSession = null,
                )
                byId[paymentIntentId] = fresh
                return GetPaymentIntentOutcome.Found(fresh)
            }
            val updated = current.copy(status = nextStatus)
            byId[paymentIntentId] = updated
            return GetPaymentIntentOutcome.Found(updated)
        }
    }
}

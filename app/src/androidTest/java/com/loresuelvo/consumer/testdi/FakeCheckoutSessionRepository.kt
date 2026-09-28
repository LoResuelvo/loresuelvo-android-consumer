package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.domain.payment.CheckoutSession
import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import com.loresuelvo.consumer.domain.payment.CheckoutPricing
import com.loresuelvo.consumer.domain.payment.PaymentIntent
import com.loresuelvo.consumer.domain.payment.PaymentIntentStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test-only [CheckoutSessionRepository] for the wire-pin
 * instrumented tests.
 *
 * Used by every `@HiltAndroidTest` that uninstalls the production
 * `RepositoryModule` and exercises a screen that triggers one of
 * the two payment flows (`payNow` on `ProposalDetailViewModel`,
 * `payNow` on `WorkOrderDetailViewModel`). The fake captures the
 * last call to [startServiceProposalCheckout] / [startWorkOrderCheckout]
 * so the test can assert that the route's wire actually
 * forwarded the user's tap to the use case — the regression
 * that the `b1cc0eb` integration test was designed to catch.
 *
 * Default behaviour: both flows return a `Created` outcome with a
 * non-empty `checkoutSession.url`, matching the production
 * happy-path contract. Tests that need a failure path call
 * [queueFailure] before tapping the pay CTA so the next call
 * surfaces the typed [CheckoutSessionOutcome.Network] /
 * [CheckoutSessionOutcome.Server] / [AlreadyPaid] variant.
 *
 * Mirrors the discipline of the other `testdi/` fakes — kept in
 * `com.loresuelvo.consumer.testdi` so every wire-pin test can
 * `import` it without dragging the package path into the test
 * source.
 */
@Singleton
class FakeCheckoutSessionRepository @Inject constructor() : CheckoutSessionRepository {
    private var startServiceOutcome: CheckoutSessionOutcome = defaultCreated()
    private var startWorkOrderOutcome: CheckoutSessionOutcome = defaultCreated()

    var lastServiceProposalId: Int? = null
        private set
    var lastWorkOrderId: Int? = null
        private set

    fun queueStartServiceProposalCheckout(outcome: CheckoutSessionOutcome) {
        startServiceOutcome = outcome
    }

    fun queueStartWorkOrderCheckout(outcome: CheckoutSessionOutcome) {
        startWorkOrderOutcome = outcome
    }

    override suspend fun startServiceProposalCheckout(serviceProposalId: Int): CheckoutSessionOutcome {
        lastServiceProposalId = serviceProposalId
        return startServiceOutcome
    }

    override suspend fun startWorkOrderCheckout(workOrderId: Int): CheckoutSessionOutcome {
        lastWorkOrderId = workOrderId
        return startWorkOrderOutcome
    }

    private companion object {
        fun defaultCreated(): CheckoutSessionOutcome.Created =
            CheckoutSessionOutcome.Created(
                intent = PaymentIntent(
                    id = "pi-1",
                    serviceProposalId = 0,
                    status = PaymentIntentStatus.CheckoutReady,
                    checkoutSession = CheckoutSession(
                        externalID = "pref-1",
                        url = "https://mp.test/checkout/42",
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
    }
}

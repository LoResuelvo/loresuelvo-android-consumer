package com.loresuelvo.consumer.domain.payment

/**
 * Port for starting a Mercado Pago Checkout Pro session for the
 * two payment flows the consumer app supports today:
 *
 *  - **Booking deposit** (US-21 confirm-agreement): the consumer
 *    books the visit by paying the deposit
 *    `POST /service-proposals/{serviceProposalID}/checkout-sessions`.
 *  - **Service balance** (US-27 `visualize-turns-detail`,
 *    scenario 09-VTD): the consumer clears the remaining balance
 *    once the provider files the completion report
 *    `POST /work-orders/{workOrderID}/checkout-sessions`.
 *
 * Both endpoints share the
 * [com.loresuelvo.consumer.data.api.dto.CheckoutSessionDto]
 * wire shape and the [CheckoutSessionOutcome] hierarchy. The
 * repository is idempotent at the application layer — if an
 * active checkout already exists, the backend returns the same
 * intent and checkout URL without creating a duplicate MP
 * preference (UNIQUE index on `payment_intents` over active
 * statuses).
 */
interface CheckoutSessionRepository {
    suspend fun startServiceProposalCheckout(serviceProposalId: Int): CheckoutSessionOutcome
    suspend fun startWorkOrderCheckout(workOrderId: Int): CheckoutSessionOutcome
}
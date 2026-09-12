package com.loresuelvo.consumer.domain.payment

/**
 * Read-only projection of a backend payment intent — the join
 * entity between a `ServiceProposal` (booking-deposit flow, US-21)
 * or a `WorkOrder` (service-balance flow, US-28) and the external
 * Mercado Pago checkout session.
 *
 * Identified by [id] (a UUID minted by the backend). [status] is
 * the authoritative value the consumer must render; the consumer
 * never transitions statuses locally (the webhook is the only
 * legitimate source of a state change). [checkoutSession] is
 * populated whenever the API returned a usable `checkout_url` —
 * when null, the consumer can poll without re-opening a Custom Tab.
 */
data class PaymentIntent(
    val id: String,
    val serviceProposalId: Int,
    val status: PaymentIntentStatus,
    val checkoutSession: CheckoutSession?,
)
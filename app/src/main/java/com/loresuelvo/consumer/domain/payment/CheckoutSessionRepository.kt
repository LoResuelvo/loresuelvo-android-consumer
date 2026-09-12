package com.loresuelvo.consumer.domain.payment

/**
 * Port for starting a Mercado Pago Checkout Pro session for the
 * booking deposit of a service proposal (US-21 confirm-agreement
 * flow). Implemented in
 * `data/api/ApiCheckoutSessionRepository.kt` against the
 * backend's authenticated
 * `POST /service-proposals/{serviceProposalID}/checkout-sessions`
 * endpoint. The repository is idempotent at the application
 * layer — if an active checkout already exists, the backend
 * returns the same intent and checkout URL without creating a
 * duplicate MP preference (UNIQUE index on
 * `payment_intents(service_proposal_id, purpose)` over active
 * statuses).
 */
interface CheckoutSessionRepository {
    suspend fun startServiceProposalCheckout(serviceProposalId: Int): CheckoutSessionOutcome
}
package com.loresuelvo.consumer.domain.payment

/**
 * Outcomes for [PaymentIntentRepository.get]. Mirrors the
 * [CheckoutSessionOutcome] shape but only carries a [PaymentIntent]
 * (no pricing — the client cached the pricing at checkout-create
 * time and only needs the status for the post-redirect polling
 * loop).
 */
sealed interface GetPaymentIntentOutcome {
    data class Found(val intent: PaymentIntent) : GetPaymentIntentOutcome
    data class Network(val cause: Throwable) : GetPaymentIntentOutcome
    data class Server(val code: Int, val message: String) : GetPaymentIntentOutcome
    data object NotFound : GetPaymentIntentOutcome
}
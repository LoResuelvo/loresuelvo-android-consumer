package com.loresuelvo.consumer.domain.payment

/**
 * Outcomes for [CheckoutSessionRepository.startServiceProposalCheckout]
 * and [CheckoutSessionRepository.startWorkOrderCheckout]. The API
 * never throws on HTTP / network failures — every transport error
 * is translated to [Failure.Network] / [Failure.Server]. When the
 * proposal/work order is already paid, the API returns `409
 * Conflict`; the implementation maps that to [Failure.AlreadyPaid]
 * so the UI can show a specific "este acuerdo ya fue confirmado"
 * copy without guessing.
 */
sealed interface CheckoutSessionOutcome {
    /**
     * The checkout was created or reused successfully. The consumer
     * should open [PaymentIntent.checkoutSession] in a Custom Tab
     * and let the user complete the payment.
     */
    data class Created(val intent: PaymentIntent, val pricing: CheckoutPricing) : CheckoutSessionOutcome

    data class Network(val cause: Throwable) : CheckoutSessionOutcome
    data class Server(val code: Int, val message: String) : CheckoutSessionOutcome
    data class AlreadyPaid(val message: String) : CheckoutSessionOutcome
}
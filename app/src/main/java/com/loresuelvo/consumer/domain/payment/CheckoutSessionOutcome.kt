package com.loresuelvo.consumer.domain.payment

/**
 * Outcomes for [CheckoutSessionRepository.startServiceProposalCheckout]
 * and [CheckoutSessionRepository.startWorkOrderCheckout]. The API
 * adapter translates transport errors to [Network] / [Server].
 * [AlreadyPaid] requires an explicit backend confirmation of payment;
 * a conflict alone can also mean an expired deadline or unavailable checkout.
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
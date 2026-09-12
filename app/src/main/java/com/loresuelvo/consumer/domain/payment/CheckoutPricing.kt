package com.loresuelvo.consumer.domain.payment

/**
 * The amount the consumer has to pay RIGHT NOW for a service
 * agreement. Mirrors the `ServiceBalanceCheckoutSession` /
 * `CheckoutSession` OpenAPI shapes; for the booking-deposit
 * scenario (US-21) the same shape is reused and the platform-fee
 * split lives in [depositCents] / [serviceTotalCents]. The
 * consumer never recomputes these numbers locally — every figure
 * comes from the backend at the moment the checkout was created.
 *
 * Time is carried as epoch millis (`Long`) to stay compatible
 * with `minSdk = 24` (no core-library desugaring enabled in this
 * project).
 */
data class CheckoutPricing(
    val currency: String,
    val serviceTotalCents: Long,
    val depositCents: Long,
    val platformFeeTotalCents: Long,
    val platformFeeDueNowCents: Long,
    val amountDueNowCents: Long,
    val bookingPaymentDeadlineEpochMillis: Long?,
)
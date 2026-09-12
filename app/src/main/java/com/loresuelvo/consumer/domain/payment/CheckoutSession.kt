package com.loresuelvo.consumer.domain.payment

/**
 * The Checkout Pro session a [PaymentIntent] is bound to. Only
 * present when the API has already called `CreateCheckout` at
 * Mercado Pago (status `CheckoutReady` and onwards). The
 * [expiresOnEpochMillis] is set by the backend (default validity
 * 30 minutes) and the consumer must not show the [url] past that
 * boundary — otherwise MP will reject the redirect with a stale
 * preference error.
 *
 * [externalID] is the `preference_id` returned by MP. Currently
 * unused by the consumer UI but exposed for debugging / future
 * analytics.
 *
 * Time is carried as epoch millis (`Long`) to stay compatible
 * with `minSdk = 24` — the project does not enable core-library
 * desugaring, so `java.time.Instant` is unavailable.
 */
data class CheckoutSession(
    val externalID: String,
    val url: String,
    val expiresOnEpochMillis: Long,
)
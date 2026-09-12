package com.loresuelvo.consumer.domain.payment

/**
 * Port for polling a single [PaymentIntent] after the user has
 * been redirected back from Mercado Pago. Implemented in
 * `data/api/ApiPaymentIntentRepository.kt` against the backend's
 * authenticated `GET /payment-intents/{paymentIntentID}`
 * endpoint. The endpoint is safe to call repeatedly — it is
 * idempotent and the returned status reflects the last verified
 * payment event processed by the backend webhook.
 */
interface PaymentIntentRepository {
    suspend fun get(paymentIntentId: String): GetPaymentIntentOutcome
}
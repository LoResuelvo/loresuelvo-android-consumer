package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for `GET /payment-intents/{id}`. The API returns
 * the current status of the payment intent — used by the
 * consumer to drive the post-redirect polling loop (US-21
 * confirmation flow + US-28 service-balance flow). The
 * `payment_intent_id` is the same value the consumer received at
 * checkout-create time, so the Android app can correlate the
 * response with the in-memory state without an extra round trip.
 */
@Serializable
data class PaymentIntentDto(
    @SerialName("id") val id: String,
    @SerialName("status") val status: String,
)
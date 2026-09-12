package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for `POST /service-proposals/{id}/checkout-sessions`
 * and `POST /work-orders/{id}/checkout-sessions`. The body is
 * `application/json` with no request body — both endpoints read
 * the resource id from the path and infer the rest from the
 * authenticated user (consumer) and the resource's current
 * state.
 *
 * The response shape mirrors the OpenAPI schema
 * `CheckoutSession` (which `ServiceBalanceCheckoutSession`
 * extends) and the two endpoints return the exact same wire
 * format for the consumer app. The proposal-id / work-order-id
 * never appears in the response — the consumer only ever needs
 * the `payment_intent_id` to poll the payment status afterwards.
 */
@Serializable
data class CheckoutSessionDto(
    @SerialName("payment_intent_id") val paymentIntentId: String,
    @SerialName("status") val status: String,
    @SerialName("checkout_url") val checkoutUrl: String,
    @SerialName("expires_on") val expiresOn: String,
    @SerialName("pricing") val pricing: CheckoutPricingDto,
)

/**
 * Wire format for the `pricing` sub-object inside
 * [CheckoutSessionDto]. Mirrors the OpenAPI
 * `ServiceBalancePricing` schema for the booking-deposit scenario
 * (which carries the same fields as the service-balance scenario
 * for the consumer's purpose). All amounts are in cents of
 * [currency]. Time fields are ISO-8601 strings (mapped to
 * epoch millis by [IsoTimestamp]).
 */
@Serializable
data class CheckoutPricingDto(
    @SerialName("currency") val currency: String,
    @SerialName("service_total_cents") val serviceTotalCents: Long? = null,
    @SerialName("deposit_cents") val depositCents: Long? = null,
    @SerialName("remaining_service_balance_cents") val remainingServiceBalanceCents: Long? = null,
    @SerialName("platform_fee_total_cents") val platformFeeTotalCents: Long? = null,
    @SerialName("platform_fee_due_now_cents") val platformFeeDueNowCents: Long? = null,
    @SerialName("remaining_platform_fee_cents") val remainingPlatformFeeCents: Long? = null,
    @SerialName("amount_due_now_cents") val amountDueNowCents: Long,
    @SerialName("remaining_amount_due_cents") val remainingAmountDueCents: Long? = null,
    @SerialName("booking_payment_deadline") val bookingPaymentDeadline: String? = null,
)
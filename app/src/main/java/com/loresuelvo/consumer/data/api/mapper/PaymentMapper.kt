package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.CheckoutPricingDto
import com.loresuelvo.consumer.data.api.dto.CheckoutSessionDto
import com.loresuelvo.consumer.data.api.dto.PaymentIntentDto
import com.loresuelvo.consumer.domain.payment.CheckoutPricing
import com.loresuelvo.consumer.domain.payment.CheckoutSession
import com.loresuelvo.consumer.domain.payment.PaymentIntent
import com.loresuelvo.consumer.domain.payment.PaymentIntentStatus

/**
 * Domain mappers for the payment layer.
 *
 * Rules:
 *  - Money fields are in cents (`Long`); no conversion happens
 *    here.
 *  - `expires_on` and `booking_payment_deadline` are ISO-8601
 *    strings — parsed to epoch millis via the shared
 *    [IsoTimestamp] helper. We keep time as `Long` to stay
 *    compatible with `minSdk = 24` (no core-library desugaring
 *    enabled).
 *  - Unknown `status` values collapse to [PaymentIntentStatus.Rejected]
 *    so the consumer UI never shows a phantom "pending" state for
 *    a status the backend invented post-our-release. The backend
 *    rejects unknown statuses upstream; this is purely defensive.
 */

internal fun PaymentIntentDto.toDomain(): PaymentIntent {
    val status = when (status.lowercase()) {
        "requires_checkout" -> PaymentIntentStatus.RequiresCheckout
        "checkout_ready" -> PaymentIntentStatus.CheckoutReady
        "processing" -> PaymentIntentStatus.Processing
        "paid" -> PaymentIntentStatus.Paid
        "rejected" -> PaymentIntentStatus.Rejected
        else -> PaymentIntentStatus.Rejected
    }
    return PaymentIntent(
        id = id,
        serviceProposalId = 0, // Not surfaced by the GET endpoint; the consumer
                                // never needs it post-redirect.
        status = status,
        checkoutSession = null,
    )
}

internal fun CheckoutSessionDto.toDomain(serviceProposalId: Int): PaymentIntent {
    val status = when (status.lowercase()) {
        "requires_checkout" -> PaymentIntentStatus.RequiresCheckout
        "checkout_ready" -> PaymentIntentStatus.CheckoutReady
        "processing" -> PaymentIntentStatus.Processing
        "paid" -> PaymentIntentStatus.Paid
        "rejected" -> PaymentIntentStatus.Rejected
        else -> PaymentIntentStatus.CheckoutReady // new checkouts land here
    }
    val expiresOnEpochMillis = parseIsoTimestampMillisOrZero(expiresOn) ?: 0L
    return PaymentIntent(
        id = paymentIntentId,
        serviceProposalId = serviceProposalId,
        status = status,
        checkoutSession = CheckoutSession(
            externalID = "", // MP preference_id is not exposed in the
                              // current response shape; left blank for
                              // now — the consumer never reads it.
            url = checkoutUrl,
            expiresOnEpochMillis = expiresOnEpochMillis,
        ),
    )
}

internal fun CheckoutPricingDto.toDomain(): CheckoutPricing = CheckoutPricing(
    currency = currency,
    serviceTotalCents = serviceTotalCents ?: 0L,
    depositCents = depositCents ?: 0L,
    platformFeeTotalCents = platformFeeTotalCents ?: 0L,
    platformFeeDueNowCents = platformFeeDueNowCents ?: 0L,
    amountDueNowCents = amountDueNowCents,
    bookingPaymentDeadlineEpochMillis = bookingPaymentDeadline
        ?.let { parseIsoTimestampMillisOrZero(it) ?: 0L },
)
package com.loresuelvo.consumer.domain.payment

data class CheckoutPricing(
    val currency: String,
    val serviceTotalCents: Long,
    val depositCents: Long,
    val platformFeeTotalCents: Long,
    val platformFeeDueNowCents: Long,
    val amountDueNowCents: Long,
    val bookingPaymentDeadlineEpochMillis: Long?,
)
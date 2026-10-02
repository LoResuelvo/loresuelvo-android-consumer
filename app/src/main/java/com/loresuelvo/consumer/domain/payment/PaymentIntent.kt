package com.loresuelvo.consumer.domain.payment

data class PaymentIntent(
    val id: String,
    val serviceProposalId: Int,
    val status: PaymentIntentStatus,
    val checkoutSession: CheckoutSession?,
)
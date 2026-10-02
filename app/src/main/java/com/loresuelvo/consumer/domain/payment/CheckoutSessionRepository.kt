package com.loresuelvo.consumer.domain.payment

interface CheckoutSessionRepository {
    suspend fun startServiceProposalCheckout(serviceProposalId: Int): CheckoutSessionOutcome
    suspend fun startWorkOrderCheckout(workOrderId: Int): CheckoutSessionOutcome
}
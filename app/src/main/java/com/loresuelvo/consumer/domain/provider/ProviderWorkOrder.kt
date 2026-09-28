package com.loresuelvo.consumer.domain.provider

data class ProviderWorkOrder(
    val id: String,
    val scheduledOnEpochMillis: Long,
    val description: String,
    val status: ProviderWorkOrderStatus,
    val completionReport: ProviderCompletionReport?,
    val review: ProviderReview?,
)

enum class ProviderWorkOrderStatus {
    Pending,
    Scheduled,
    AwaitingPayment,
    Paid,
    Finished,
    Cancelled,
    Unknown,
}

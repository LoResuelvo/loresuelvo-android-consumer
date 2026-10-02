package com.loresuelvo.consumer.domain.serviceproposal

data class ServiceProposal(
    val id: String,
    val conversationId: String?,
    val status: ServiceProposalStatus,
    val counterpart: ServiceProposalCounterpart,
    val description: String,
    val amountCents: Long,
    val scheduledOnEpochMillis: Long,
    val createdOnEpochMillis: Long,
    val estimatedDurationMinutes: Int? = null,
)

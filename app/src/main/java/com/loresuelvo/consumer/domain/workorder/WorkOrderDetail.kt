package com.loresuelvo.consumer.domain.workorder

import com.loresuelvo.consumer.domain.turno.TurnoStatus

data class WorkOrderDetail(
    val proposalId: String,
    val provider: WorkOrderDetailCounterpart,
    val description: String,
    val amountCents: Long,
    val scheduledOnEpochMillis: Long,
    val acceptedOnEpochMillis: Long,
    val paidOnEpochMillis: Long?,
    val status: TurnoStatus,
    val completionReport: CompletionReport?,
    val review: WorkOrderReview?,
    val estimatedDurationMinutes: Int?,
)
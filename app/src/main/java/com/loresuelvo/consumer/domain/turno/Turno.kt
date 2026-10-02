package com.loresuelvo.consumer.domain.turno

data class Turno(
    val id: String,
    val serviceProposalId: String,
    val status: TurnoStatus,
    val counterpart: TurnoCounterpart,
    val description: String,
    val amountCents: Long,
    val scheduledOnEpochMillis: Long,
)

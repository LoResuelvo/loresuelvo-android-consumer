package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.TurnoCounterpartDto
import com.loresuelvo.consumer.data.api.dto.TurnoDto
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus

internal fun TurnoDto.toDomain(): Turno? {
    val status = when (status.lowercase()) {
        "scheduled" -> TurnoStatus.Confirmed
        "awaiting_payment" -> TurnoStatus.AwaitingPayment
        "paid" -> TurnoStatus.Paid
        else -> return null
    }
    return Turno(
        id = id.toString(),
        serviceProposalId = serviceProposalId.toString(),
        status = status,
        counterpart = counterpart.toDomain(),
        description = description,
        amountCents = amountCents,
        scheduledOnEpochMillis = parseIsoTimestampMillisOrZero(scheduledOn) ?: 0L,
    )
}

internal fun TurnoCounterpartDto.toDomain(): TurnoCounterpart =
    TurnoCounterpart(
        id = id.toString(),
        name = name,
        surname = surname,
        categoryName = categoryName,
        profilePhotoUrl = profilePhotoUrl,
    )

internal fun List<TurnoDto>.toDomain(): List<Turno> = mapNotNull { it.toDomain() }

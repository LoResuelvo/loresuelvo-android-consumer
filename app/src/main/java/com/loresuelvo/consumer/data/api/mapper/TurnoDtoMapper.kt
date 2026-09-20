package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.TurnoCounterpartDto
import com.loresuelvo.consumer.data.api.dto.TurnoDto
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus

/**
 * DTO → domain translation for the `GET /work-orders` endpoint
 * (visualize-turns.feature scenario 02-VT).
 *
 * Mapping rules:
 * - `Long` ids on the wire become `String` in the domain.
 *   `amount_cents` stays `Long`.
 * - `scheduled_on` is parsed through `parseIsoTimestampMillisOrZero`;
 *   on a malformed timestamp the mapper collapses to `0L`.
 * - `status` is normalised lowercase and mapped to [TurnoStatus].
 *   Today only `"scheduled"` is known — mapped to
 *   [TurnoStatus.Confirmed]. Unknown statuses return `null` from
 *   the single-element mapper and the list overload filters
 *   them out via `mapNotNull`.
 * - `counterpart.role` is intentionally NOT mapped.
 */
internal fun TurnoDto.toDomain(): Turno? {
    val status = when (status.lowercase()) {
        "scheduled" -> TurnoStatus.Confirmed
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

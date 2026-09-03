package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ServiceProposalCounterpartDto
import com.loresuelvo.consumer.data.api.dto.ServiceProposalDto
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus

/**
 * DTO -> domain translation for the `GET /service-proposals`
 * endpoint.
 *
 * Mapping rules (each pinned by
 * [com.loresuelvo.consumer.data.api.mapper.ServiceProposalDtoMapperTest]):
 *
 * - `Long` ids on the wire become `String` in the domain, mirroring
 *   the convention used by `JobRequest` and `Conversation` (stable
 *   LazyColumn keys, no overflow concerns). `amount_cents` stays
 *   `Long` because it is a value.
 * - `scheduled_on` / `created_on` are parsed through
 *   [parseIsoTimestampMillisOrZero]; on a malformed timestamp the
 *   mapper collapses to `0L` so the row still renders (matching
 *   the other mappers' behaviour).
 * - `status` is normalised lowercase and mapped to
 *   [ServiceProposalStatus]. Unknown statuses return `null` from
 *   the single-element mapper and the list overload filters them
 *   out via [mapNotNull] — the consumer never sees a proposal it
 *   cannot classify.
 * - `counterpart.role` is intentionally NOT mapped: the
 *   Home / Mis Servicios surfaces always render the proposal from
 *   the consumer's perspective, where the counterpart is always
 *   the provider. The field is read by the DTO (so
 *   `ignoreUnknownKeys` does not need a per-endpoint carve-out)
 *   but discarded.
 */
internal fun ServiceProposalDto.toDomain(): ServiceProposal? {
    val status = when (status.lowercase()) {
        "pending" -> ServiceProposalStatus.Pending
        "accepted" -> ServiceProposalStatus.Accepted
        "rejected" -> ServiceProposalStatus.Rejected
        else -> return null
    }
    return ServiceProposal(
        id = id.toString(),
        conversationId = conversationId?.toString(),
        status = status,
        counterpart = counterpart.toDomain(),
        description = description,
        amountCents = amountCents,
        scheduledOnEpochMillis = parseIsoTimestampMillisOrZero(scheduledOn) ?: 0L,
        createdOnEpochMillis = parseIsoTimestampMillisOrZero(createdOn) ?: 0L,
    )
}

internal fun ServiceProposalCounterpartDto.toDomain(): ServiceProposalCounterpart =
    ServiceProposalCounterpart(
        id = id.toString(),
        name = name,
        surname = surname,
        categoryName = categoryName,
        profilePhotoUrl = profilePhotoUrl,
    )

internal fun List<ServiceProposalDto>.toDomain(): List<ServiceProposal> =
    mapNotNull { it.toDomain() }

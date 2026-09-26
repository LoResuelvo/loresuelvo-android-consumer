package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.CompletionReportDto
import com.loresuelvo.consumer.data.api.dto.CompletionReportPhotoDto
import com.loresuelvo.consumer.data.api.dto.ReviewDto
import com.loresuelvo.consumer.data.api.dto.WorkOrderDetailCounterpartDto
import com.loresuelvo.consumer.data.api.dto.WorkOrderDetailDto
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.workorder.CompletionReport
import com.loresuelvo.consumer.domain.workorder.CompletionReportPhoto
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderReview

/**
 * DTO → domain translation for the `GET /work-orders/{workOrderID}`
 * endpoint (US-27 `visualize-turns-detail`).
 *
 * Mapping rules:
 * - `Long` ids on the wire become `String` in the domain (stable
 *   `LazyColumn` keys, no overflow concerns).
 * - `scheduled_on` / `accepted_on` / `paid_on` /
 *   `completion_report.reported_on` arrive as ISO-8601 strings
 *   with a trailing `Z`; the mapper parses them via
 *   `parseIsoTimestampMillisOrZero`. On a malformed timestamp the
 *   mapper collapses to `0L` (or `null` for optional fields).
 * - `status` is normalised lowercase and mapped to
 *   [TurnoStatus]. Known values:
 *   - `scheduled`         → [TurnoStatus.Confirmed]
 *   - `awaiting_payment`  → [TurnoStatus.AwaitingPayment]
 *   - `paid`              → [TurnoStatus.Paid]
 *   - unknown statuses collapse to `null` from the single
 *     mapper so the caller (the repository adapter) decides
 *     whether to surface the failure.
 * - `provider.role` (always `"provider"`) is decoded but
 *   intentionally NOT mapped (the domain type does not need
 *   the role).
 *
 * `consumer_id` and `provider_id` are surfaced on the wire but
 * not mapped today: the consumer-facing detail screen renders
 * the `WorkOrderDetailCounterpart` block which already carries
 * the provider identity. A future US can surface them if the
 * consumer starts querying work-orders they didn't author.
 */
internal fun WorkOrderDetailDto.toDomain(
    fallbackProvider: WorkOrderDetailCounterpart? = null,
): WorkOrderDetail? {
    val status = when (status.lowercase()) {
        "scheduled" -> TurnoStatus.Confirmed
        "awaiting_payment" -> TurnoStatus.AwaitingPayment
        "paid" -> TurnoStatus.Paid
        // TODO(US-27 follow-up): map `finished` / `cancelled`
        // once the dedicated endpoint widens to surface them.
        else -> return null
    }
    val provider = fallbackProvider ?: return null
    return WorkOrderDetail(
        proposalId = serviceProposalId.toString(),
        provider = provider,
        description = description,
        amountCents = amountCents,
        scheduledOnEpochMillis = parseIsoTimestampMillisOrZero(scheduledOn) ?: 0L,
        acceptedOnEpochMillis = parseIsoTimestampMillisOrZero(acceptedOn) ?: 0L,
        paidOnEpochMillis = paidOn?.let { parseIsoTimestampMillisOrZero(it) },
        status = status,
        completionReport = completionReport?.toDomain(),
        review = review?.toDomain(),
        estimatedDurationMinutes = null,
    )
}

internal fun WorkOrderDetailCounterpartDto.toDomain(): WorkOrderDetailCounterpart =
    WorkOrderDetailCounterpart(
        id = id.toString(),
        name = name,
        surname = surname,
        categoryName = categoryName,
        profilePhotoUrl = profilePhotoUrl,
    )

internal fun CompletionReportDto.toDomain(): CompletionReport =
    CompletionReport(
        id = id.toString(),
        description = description,
        reportedOnEpochMillis = parseIsoTimestampMillisOrZero(reportedOn) ?: 0L,
        images = images.map { it.toDomain() },
    )

internal fun CompletionReportPhotoDto.toDomain(): CompletionReportPhoto =
    CompletionReportPhoto(
        fileId = fileId,
        originalName = originalName,
        url = url,
    )

internal fun ReviewDto.toDomain(): WorkOrderReview =
    WorkOrderReview(
        rating = rating,
        description = description,
    )

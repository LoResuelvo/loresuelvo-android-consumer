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

internal fun WorkOrderDetailDto.toDomain(
    fallbackProvider: WorkOrderDetailCounterpart? = null,
): WorkOrderDetail? {
    val status = when (status.lowercase()) {
        "scheduled" -> TurnoStatus.Confirmed
        "awaiting_payment" -> TurnoStatus.AwaitingPayment
        "paid" -> TurnoStatus.Paid
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
        identityVerified = identityVerified,
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

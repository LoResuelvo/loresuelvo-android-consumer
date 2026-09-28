package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ProviderProfileDto
import com.loresuelvo.consumer.data.api.dto.ProviderProfileWorkOrderDto
import com.loresuelvo.consumer.domain.provider.ProviderCategory
import com.loresuelvo.consumer.domain.provider.ProviderCompletionReport
import com.loresuelvo.consumer.domain.provider.ProviderProfile
import com.loresuelvo.consumer.domain.provider.ProviderReview
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrder
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrderStatus

internal fun ProviderProfileDto.toDomain(): ProviderProfile = ProviderProfile(
    id = id,
    name = name,
    surname = surname,
    profilePhotoUrl = profilePhoto?.url,
    category = ProviderCategory(
        id = category.id,
        name = category.name,
    ),
    ratingAverage = ratingAverage,
    ratingCount = ratingCount,
    identityVerified = identityVerified,
    workOrders = workOrders.map(ProviderProfileWorkOrderDto::toDomain),
)

private fun ProviderProfileWorkOrderDto.toDomain(): ProviderWorkOrder = ProviderWorkOrder(
    id = id.toString(),
    scheduledOnEpochMillis = parseIsoTimestampMillisOrZero(scheduledOn) ?: 0L,
    description = description,
    status = when (status.lowercase()) {
        "pending" -> ProviderWorkOrderStatus.Pending
        "scheduled" -> ProviderWorkOrderStatus.Scheduled
        "awaiting_payment" -> ProviderWorkOrderStatus.AwaitingPayment
        "paid" -> ProviderWorkOrderStatus.Paid
        "finished" -> ProviderWorkOrderStatus.Finished
        "cancelled" -> ProviderWorkOrderStatus.Cancelled
        else -> ProviderWorkOrderStatus.Unknown
    },
    completionReport = completionReport?.let {
        ProviderCompletionReport(
            description = it.description,
            reportedOnEpochMillis = parseIsoTimestampMillisOrZero(it.reportedOn) ?: 0L,
        )
    },
    review = review?.let {
        ProviderReview(
            rating = it.rating,
            description = it.description,
        )
    },
)

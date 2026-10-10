package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkOrderDetailDto(
    @SerialName("id") val id: Long,
    @SerialName("service_proposal_id") val serviceProposalId: Long,
    @SerialName("consumer_id") val consumerId: Long,
    @SerialName("provider_id") val providerId: Long,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("scheduled_on") val scheduledOn: String,
    @SerialName("description") val description: String,
    @SerialName("status") val status: String,
    @SerialName("accepted_on") val acceptedOn: String? = null,
    @SerialName("paid_on") val paidOn: String? = null,
    @SerialName("completion_report") val completionReport: CompletionReportDto? = null,
    @SerialName("review") val review: ReviewDto? = null,
)

@Serializable
data class CompletionReportDto(
    @SerialName("id") val id: Long,
    @SerialName("description") val description: String,
    @SerialName("reported_on") val reportedOn: String,
    @SerialName("images") val images: List<CompletionReportPhotoDto>,
)

@Serializable
data class CompletionReportPhotoDto(
    @SerialName("file_id") val fileId: String,
    @SerialName("original_name") val originalName: String,
    @SerialName("url") val url: String,
)

@Serializable
data class WorkOrderDetailCounterpartDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("surname") val surname: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    @SerialName("identity_verified") val identityVerified: Boolean = false,
)

@Serializable
data class ReviewDto(
    @SerialName("rating") val rating: Int,
    @SerialName("description") val description: String,
)

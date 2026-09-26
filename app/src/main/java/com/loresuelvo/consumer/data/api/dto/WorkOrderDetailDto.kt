package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for `GET /work-orders/{workOrderID}`
 * (US-27 `visualize-turns-detail`).
 *
 * Fields are conditional on the [status] value:
 *  - `scheduled`         → only the top-level fields. No
 *    `paid_on`, `completion_report`, `review`.
 *  - `awaiting_payment`  → adds `completion_report` (provider
 *    filed the report, consumer must clear the remaining
 *    balance).
 *  - `paid`              → adds `paid_on`, `completion_report`
 *    and `review` (consumer submitted a review).
 *
 * Field-name mapping rules:
 *  - `id`, `service_proposal_id`, `consumer_id`,
 *    `provider_id`, `completion_report.id` arrive as `Long` on
 *    the wire and become `String` in the domain (stable
 *    `LazyColumn` keys, no overflow concerns).
 *  - `scheduled_on`, `accepted_on`, `paid_on`,
 *    `completion_report.reported_on` arrive as ISO-8601
 *    strings with a trailing `Z`; the mapper parses them via
 *    `data/api/mapper/IsoTimestamp.kt`.
 *  - `amount_cents` stays `Long` (a value, not an identifier).
 *  - `provider.role` is decoded but intentionally ignored.
 *
 * The endpoint requires a valid Auth0 JWT (the `AuthInterceptor`
 * injects the bearer token automatically).
 */
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
)

@Serializable
data class ReviewDto(
    @SerialName("rating") val rating: Int,
    @SerialName("description") val description: String,
)

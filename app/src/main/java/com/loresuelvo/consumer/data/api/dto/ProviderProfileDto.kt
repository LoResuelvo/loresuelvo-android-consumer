package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProviderProfileDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("surname") val surname: String,
    @SerialName("profile_photo") val profilePhoto: ProviderProfilePhotoDto? = null,
    @SerialName("category") val category: ProviderProfileCategoryDto,
    @SerialName("rating_average") val ratingAverage: Double,
    @SerialName("rating_count") val ratingCount: Int,
    @SerialName("identity_verified") val identityVerified: Boolean,
    @SerialName("work_orders") val workOrders: List<ProviderProfileWorkOrderDto> = emptyList(),
)

@Serializable
data class ProviderProfilePhotoDto(
    @SerialName("original_name") val originalName: String,
    @SerialName("url") val url: String,
)

@Serializable
data class ProviderProfileCategoryDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
)

@Serializable
data class ProviderProfileWorkOrderDto(
    @SerialName("id") val id: Long,
    @SerialName("scheduled_on") val scheduledOn: String,
    @SerialName("description") val description: String,
    @SerialName("status") val status: String,
    @SerialName("completion_report") val completionReport: ProviderProfileCompletionReportDto? = null,
    @SerialName("review") val review: ProviderProfileReviewDto? = null,
)

@Serializable
data class ProviderProfileCompletionReportDto(
    @SerialName("description") val description: String,
    @SerialName("reported_on") val reportedOn: String,
)

@Serializable
data class ProviderProfileReviewDto(
    @SerialName("rating") val rating: Int,
    @SerialName("description") val description: String,
)

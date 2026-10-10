package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceProposalDto(
    @SerialName("id") val id: Long,
    @SerialName("conversation_id") val conversationId: Long? = null,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("scheduled_on") val scheduledOn: String,
    @SerialName("description") val description: String,
    @SerialName("status") val status: String,
    @SerialName("created_on") val createdOn: String,
    @SerialName("counterpart") val counterpart: ServiceProposalCounterpartDto,
    @SerialName("estimated_duration_minutes") val estimatedDurationMinutes: Int? = null,
)

@Serializable
data class ServiceProposalCounterpartDto(
    @SerialName("id") val id: Long,
    @SerialName("role") val role: String,
    @SerialName("name") val name: String,
    @SerialName("surname") val surname: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    @SerialName("identity_verified") val identityVerified: Boolean = false,
)

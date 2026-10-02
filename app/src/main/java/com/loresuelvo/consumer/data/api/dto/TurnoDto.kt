package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TurnoDto(
    @SerialName("id") val id: Long,
    @SerialName("service_proposal_id") val serviceProposalId: Long,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("scheduled_on") val scheduledOn: String,
    @SerialName("description") val description: String,
    @SerialName("status") val status: String,
    @SerialName("counterpart") val counterpart: TurnoCounterpartDto,
)

@Serializable
data class TurnoCounterpartDto(
    @SerialName("id") val id: Long,
    @SerialName("role") val role: String,
    @SerialName("name") val name: String,
    @SerialName("surname") val surname: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
)

package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterConsumerResponseDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val firstName: String,
    @SerialName("surname") val lastName: String,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
)

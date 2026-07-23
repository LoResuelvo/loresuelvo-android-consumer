package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentUserDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val firstName: String,
    @SerialName("surname") val lastName: String,
    @SerialName("email") val email: String,
    @SerialName("role") val role: String,
    @SerialName("profile_photo") val profilePhoto: CurrentUserProfilePhotoDto? = null,
)

@Serializable
data class CurrentUserProfilePhotoDto(
    @SerialName("original_name") val originalName: String,
    @SerialName("url") val url: String,
)

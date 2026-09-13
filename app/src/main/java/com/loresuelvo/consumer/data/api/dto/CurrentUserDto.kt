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
    @SerialName("address") val address: CurrentUserAddressDto? = null,
)

@Serializable
data class CurrentUserProfilePhotoDto(
    @SerialName("id") val id: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("key") val key: String? = null,
)

@Serializable
data class CurrentUserAddressDto(
    @SerialName("street") val street: String,
    @SerialName("street_number") val streetNumber: String,
    @SerialName("floor") val floor: String = "",
    @SerialName("unit") val unit: String = "",
)
package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentUserDto(
    @SerialName("AuthID") val authId: String,
    @SerialName("Name") val firstName: String,
    @SerialName("Surname") val lastName: String,
    @SerialName("Email") val email: String,
    @SerialName("Role") val role: String,
)

package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectCalendarRequestDto(
    @SerialName("server_auth_code") val serverAuthCode: String,
)

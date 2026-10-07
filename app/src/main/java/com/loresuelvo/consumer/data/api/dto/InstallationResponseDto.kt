package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InstallationResponseDto(
    @SerialName("installation_id") val installationId: String,
    @SerialName("binding_id") val bindingId: String,
    val app: String,
    val locale: String,
    val enabled: Boolean,
)

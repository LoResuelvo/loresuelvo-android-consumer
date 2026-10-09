package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoveInstallationRequestDto(
    @SerialName("installation_secret") val installationSecret: String,
    @SerialName("binding_id") val bindingId: String,
)

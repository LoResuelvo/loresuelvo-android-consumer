package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterInstallationRequestDto(
    @SerialName("installation_secret") val installationSecret: String,
    val app: String,
    @SerialName("fcm_token") val fcmToken: String,
    val locale: String,
    @SerialName("binding_id") val bindingId: String,
    @SerialName("previous_binding_id") val previousBindingId: String? = null,
)

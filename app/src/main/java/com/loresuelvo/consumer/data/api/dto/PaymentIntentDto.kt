package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentIntentDto(
    @SerialName("id") val id: String,
    @SerialName("status") val status: String,
)
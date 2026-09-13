package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterConsumerAddressDto(
    @SerialName("street")
    val street: String,

    @SerialName("street_number")
    val streetNumber: String,

    @SerialName("floor")
    val floor: String,

    @SerialName("unit")
    val unit: String,
)
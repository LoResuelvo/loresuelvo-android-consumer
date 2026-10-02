package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmitReviewRequestDto(
    @SerialName("rating") val rating: Int,
    @SerialName("description") val description: String,
)

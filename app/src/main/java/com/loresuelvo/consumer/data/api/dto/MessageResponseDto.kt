package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for endpoints that return a single human-readable
 * message. Used by `POST /consumers` (201 success) and similar
 * acknowledgement-style responses across the API.
 *
 * Spec: `openapi/components/schemas/message-response.yaml`.
 *   required: [message]
 *   additionalProperties: false
 */
@Serializable
data class MessageResponseDto(
    @SerialName("message") val message: String,
)

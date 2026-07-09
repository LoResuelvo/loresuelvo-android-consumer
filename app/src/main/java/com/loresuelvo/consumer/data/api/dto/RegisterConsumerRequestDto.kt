package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for `POST /consumers`. The domain uses camelCase
 * ([com.loresuelvo.consumer.domain.auth.RegisterConsumerData] has
 * `firstName` / `lastName`); the backend expects snake_case `name`
 * and `surname`. The mismatch is bridged here with `@SerialName` and
 * by the mapper in `data/api/mapper/UserDtoMapper.kt`.
 *
 * Spec: `openapi/components/schemas/register-consumer-request.yaml`.
 *   required: [email]
 *   properties: email (string, format=email), name (string),
 *               surname (string). Server does not enforce non-empty
 *               validation on name / surname.
 */
@Serializable
data class RegisterConsumerRequestDto(
    @SerialName("email") val email: String,
    @SerialName("name") val firstName: String,
    @SerialName("surname") val surname: String,
)

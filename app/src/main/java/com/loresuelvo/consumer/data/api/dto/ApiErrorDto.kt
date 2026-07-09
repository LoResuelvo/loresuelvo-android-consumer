package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for every non-2xx error response. Covers two
 * response shapes documented in the OpenAPI spec:
 *
 * - `error-response.yaml`: required `error: string`, no `message`.
 * - `auth-error-response.yaml`: required `error: string` (enum),
 *   optional `message: string` (present in JWT middleware failures).
 *
 * `message` is nullable because the simpler shape does not include
 * it. Callers should fall back to `error` when `message` is null.
 * `additionalProperties: false` is enforced server-side, not
 * client-side; this DTO accepts unknown keys because
 * `Json { ignoreUnknownKeys = true }` is configured globally.
 */
@Serializable
data class ApiErrorDto(
    @SerialName("error") val error: String,
    @SerialName("message") val message: String? = null,
)

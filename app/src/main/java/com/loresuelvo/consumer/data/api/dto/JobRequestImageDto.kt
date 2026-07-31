package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape for an image attached to a [JobRequestDto]. The
 * `url` is a presigned URL scoped to the conversation — the
 * application does not cache it. `original_name` is the
 * human-readable filename the consumer uploaded.
 */
@Serializable
data class JobRequestImageDto(
    @SerialName("id") val id: String,
    @SerialName("url") val url: String,
    @SerialName("original_name") val originalName: String,
)

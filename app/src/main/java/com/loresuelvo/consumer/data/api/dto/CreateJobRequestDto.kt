package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /job-requests`. The wire shape mirrors
 * the swagger contract the user pasted on 2026-07-28:
 * `provider_id` is the source of truth for the target provider
 * (the consumer is taken from the Auth0 session by the backend),
 * `title` and `description` are the free-form fields the consumer
 * filled in the modal, and `image_file_ids` is an optional list
 * of presigned upload ids — the modal form does not expose image
 * upload today, so the field is omitted from the payload when
 * the list is empty (matches the backend's `null` semantics).
 *
 * `kotlinx-serialization`'s `explicitNulls = false` (set on the
 * shared `Json` instance in `di/NetworkModule`) drops the
 * `image_file_ids` key entirely when the field is `null`,
 * which is what the backend expects.
 */
@Serializable
data class CreateJobRequestDto(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("image_file_ids") val imageFileIds: List<String>? = null,
)

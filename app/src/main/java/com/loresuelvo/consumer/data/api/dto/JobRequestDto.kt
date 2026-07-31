package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for the `POST /job-requests` response. The backend
 * returns the freshly-persisted job request plus the conversation
 * id that the UI navigates to on success.
 *
 * `id` and `conversation_id` are `Long` on the wire. The mapper
 * converts them to `String` in the domain so `id` can be used as
 * a stable LazyColumn key without overflow concerns, mirroring
 * the convention used by `DiagnosisDto` and `ProviderDto`.
 *
 * `images` is `null` when the request carried no attachments; the
 * mapper collapses `null` to `emptyList()` so the domain type
 * stays non-nullable. The first release of the form does not
 * expose image upload, so this field is always empty in
 * practice.
 */
@Serializable
data class JobRequestDto(
    @SerialName("id") val id: Long,
    @SerialName("conversation_id") val conversationId: Long? = null,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("status") val status: String,
    @SerialName("images") val images: List<JobRequestImageDto>? = null,
)

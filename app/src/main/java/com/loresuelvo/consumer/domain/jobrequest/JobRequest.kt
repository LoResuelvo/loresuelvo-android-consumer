package com.loresuelvo.consumer.domain.jobrequest

/**
 * Job request submitted by a consumer to a provider. Created via
 * `POST /job-requests`; the first message of the resulting
 * conversation between consumer and provider carries the title
 * and description captured here.
 *
 * Pure domain type: camelCase, no framework deps. The wire format
 * (snake_case + `Long` ids + nested `images`) is mapped in
 * `data/api/mapper/JobRequestDtoMapper.kt`.
 *
 * `images` is empty for the first release — the modal form does
 * not expose image upload today. The field is kept on the entity
 * so the response can flow through unmodified when a future
 * iteration adds attachments.
 *
 * `status` is currently a free-form string (the backend returns
 * `"pending"` on create). It is NOT modelled as a sealed enum yet
 * because the only known value is `pending`; if the conversation
 * status opens up (e.g. `accepted`, `rejected`) in a future US
 * we'll convert it to a sealed type.
 */
data class JobRequest(
    val id: String,
    val conversationId: String?,
    val title: String,
    val description: String,
    val status: String,
    val images: List<JobRequestImage>,
)

package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /chatbot/conversations/{conversationId}/job-requests`.
 *
 * The wire shape mirrors the curl the user pasted on 2026-08-10
 * (dev backend, debug build):
 * ```
 *   curl -X POST http://localhost:8080/chatbot/conversations/1/job-requests
 *     -H 'Content-Type: application/json'
 *     -d '{ "provider_id": 1073741824 }'
 * ```
 * Only `provider_id` is required — the backend's AI pre-fills
 * `title` and `description` from the conversation history.
 * `provider_id` is the source of truth for the target provider;
 * the consumer identity is taken from the Auth0 session.
 *
 * Distinct from `CreateJobRequestDto` (the user-typed
 * `POST /job-requests` endpoint): the OLD flow sends `title`,
 * `description`, and `image_file_ids`; the NEW flow only sends
 * the provider id.
 */
@Serializable
data class CreateAiJobRequestRequestDto(
    @SerialName("provider_id") val providerId: Int,
)

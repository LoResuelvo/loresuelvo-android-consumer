package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape for the AI diagnostic chat backend. Mirrors the
 * backend's `POST /chatbot/conversations` and
 * `POST /chatbot/conversations/{id}/messages` responses (Go side
 * returns numeric ids; the webapp's
 * `ApiAiConversationDetail` mirrors the same shape).
 *
 * `id` is the conversation id (`Long?` because a future endpoint
 * variant may omit it before persisting). The mapper converts
 * it to the domain's `String?` so the UI can use it as a stable
 * `LazyColumn` key without overflowing.
 *
 * `messages` is the full conversation history returned by the
 * backend — the optimistic append is REPLACED by this list once
 * the round-trip succeeds. Optional metadata (`title`, `status`,
 * `response_status`, `assessment`, `response`,
 * `recommended_providers`) is intentionally ignored by the mapper
 * for now; commitments 09-DIA → 11-DIA flesh out the
 * assessment / recommendations path.
 */
@Serializable
data class DiagnosisDto(
    @SerialName("id") val id: Long? = null,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("response_status") val responseStatus: String? = null,
    @SerialName("messages") val messages: List<ChatMessageDto>,
)

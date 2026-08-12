package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape for `GET /chatbot/conversations` (the row-level
 * summary the "Asistente IA" tab renders). The list endpoint
 * returns a JSON array of these summaries; the full chat thread
 * (messages + assessment + recommended providers) is fetched on
 * tap via a separate path that maps to `DiagnosisDto`.
 *
 * `id` is `Long` on the wire (the backend's `chatbot_conversations`
 * table uses a numeric primary key). The domain mapper
 * stringifies it so the UI's `LazyColumn` keys stay stable across
 * the long > 2^63 range.
 *
 * `updated_on` is the ISO timestamp the backend uses to order the
 * list (timestamp-descending by the SQL definition). The mapper
 * parses it to epoch millis.
 *
 * `last_message` is an embedded [ChatMessageDto] (id, sender_role,
 * content, created_on, …) — the same wire shape the chat thread
 * uses. It is optional on the wire: the backend may omit it for
 * conversations that have only a system-level welcome message.
 * The mapper flattens it to the message `content` for the row
 * preview.
 *
 * `status` and `response_status` are optional strings; the future
 * "Continuar chat" vs "Ver resumen" UI affordance can be derived
 * from `assessment.outcome` (collected separately on the detail
 * endpoint, not in this list summary).
 */
@Serializable
data class AiConversationSummaryDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("updated_on") val updatedOn: String,
    @SerialName("status") val status: String? = null,
    @SerialName("response_status") val responseStatus: String? = null,
    @SerialName("last_message") val lastMessage: ChatMessageDto? = null,
)

package com.loresuelvo.consumer.domain.conversation

/**
 * Full snapshot of a single conversation, as returned by
 * `GET /conversations/{id}`. Mirrors the shape of the [Conversation]
 * list-summary type but adds the complete `messages` history so
 * the chat surface can render the entire thread without a second
 * round-trip per bubble.
 *
 * The list-summary type ([Conversation]) and the detail type are
 * deliberately modelled as separate entities rather than a single
 * class with an optional `messages` field — the wire contracts
 * differ enough (list emits `last_message`, detail emits
 * `messages[]`) and conflating them would force every list call
 * to carry an empty list (semantically noisy) or every detail
 * call to discard the full history (data loss).
 *
 *  - [id] matches the backend-issued conversation id (the same
 *    value `POST /job-requests` returned as `conversationId`).
 *  - [status] / [counterpart] / [updatedOnEpochMillis] are
 *    field-for-field identical to [Conversation]; the mapper in
 *    `data/api/mapper/ConversationDtoMapper.kt` projects them
 *    from the same JSON keys.
 *  - [messages] is the full ordered thread (oldest first); the
 *    list cell never reads from this collection — only the
 *    detail chat surface does.
 *
 * Pure domain.
 */
data class ConversationDetail(
    val id: String,
    val status: ConversationStatus,
    val counterpart: ConversationCounterpart,
    val messages: List<ConversationMessage>,
    val updatedOnEpochMillis: Long,
)
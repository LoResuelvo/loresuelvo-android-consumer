package com.loresuelvo.consumer.domain.conversation

/**
 * One entry in the consumer's conversation list, as returned by
 * `GET /conversations`. Each row pairs the conversation metadata
 * (status, counterpart) with the most recent message preview so
 * the list cell can render "Prestador · last message · time" without
 * a second round-trip per row.
 *
 *  - [id] is the backend-issued string id (the same one that
 *    `POST /job-requests` returned as `conversationId`).
 *  - [status] discriminates the request lifecycle (currently only
 *    `Pending` is meaningful for this app; unknown values fall
 *    back to [ConversationStatus.Other] so the row still renders).
 *  - [counterpart] is always the **provider** from the consumer's
 *    perspective — the consumer never lists themselves as a
 *    counterpart.
 *  - [lastMessage] may be absent for a brand-new conversation that
 *    the backend returned before persisting the first message; the
 *    list cell must tolerate `null` (render an empty preview line
 *    or hide the bubble).
 *  - [updatedOnEpochMillis] is the backend's `updated_on` parsed
 *    to millis (UTC). The UI converts it to a localized "hh:mm" /
 *    "Yesterday" / "dd MMM" string per platform conventions.
 *
 * Pure domain: camelCase, no Android, no kotlinx-serialization, no
 * Hilt, no Retrofit.
 */
data class Conversation(
    val id: String,
    val status: ConversationStatus,
    val counterpart: ConversationCounterpart,
    val lastMessage: ConversationMessage?,
    val updatedOnEpochMillis: Long,
)
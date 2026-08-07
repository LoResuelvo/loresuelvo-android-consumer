package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ConversationCounterpartDto
import com.loresuelvo.consumer.data.api.dto.ConversationDto
import com.loresuelvo.consumer.data.api.dto.ConversationMessageDto
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus

/**
 * DTO → domain translation for the consumer ↔ provider
 * conversations backend. snake_case ↔ camelCase lives here per
 * AGENTS.md; the domain never sees wire types.
 *
 * Mirrors the discipline of [DiagnosisDtoMapper]: each mapping
 * decision is pinned by a test in
 * `data/api/mapper/ConversationDtoMapperTest.kt` so a backend
 * shape drift is caught immediately.
 */

internal fun ConversationDto.toDomain(): Conversation = Conversation(
    id = id.toString(),
    status = status.toConversationStatus(),
    counterpart = counterpart.toDomain(),
    lastMessage = lastMessage?.toDomain(),
    updatedOnEpochMillis = parseIsoMillisOrZero(updatedOn) ?: 0L,
)

internal fun ConversationCounterpartDto.toDomain(): ConversationCounterpart =
    ConversationCounterpart(
        id = id,
        name = name,
        surname = surname,
        categoryName = categoryName,
        // `profile_photo_url` is null/blank for providers who have
        // not uploaded a photo; the UI falls back to an initial
        // avatar (see `ProviderAvatar`).
        profilePhotoUrl = profilePhotoUrl?.takeIf { it.isNotBlank() },
    )

internal fun ConversationMessageDto.toDomain(): ConversationMessage {
    val sender = when (senderRole.lowercase()) {
        "consumer" -> ConversationSender.Consumer
        "provider" -> ConversationSender.Provider
        // Defensive: a future sender_role renders as a Provider
        // bubble (left side) so the user can still see the message
        // body. A `Log.w` is intentionally not emitted here to
        // honour the "no log spam" rule; the eventual UX can
        // surface a system event for unknown roles.
        else -> ConversationSender.Provider
    }
    return ConversationMessage(
        id = id.toString(),
        sender = sender,
        content = content,
        createdOnEpochMillis = parseIsoMillisOrZero(createdOn) ?: 0L,
    )
}

internal fun String.toConversationStatus(): ConversationStatus =
    when (lowercase()) {
        "pending" -> ConversationStatus.Pending
        // Forward-compatible bucket for `accepted`, `rejected`,
        // `closed`, … that the backend may introduce later. The
        // raw string is kept so the UI can render it verbatim
        // without crashing on a backend revision.
        else -> ConversationStatus.Other(this)
    }

/**
 * Best-effort ISO-8601 parser for the backend's
 * `YYYY-MM-DDTHH:MM:SS[Z]` shape. We can't use `java.time.Instant`
 * because `minSdk = 24`; `SimpleDateFormat` is API-1 friendly and
 * sufficient for the conversations wire.
 *
 * Tries the trailing-`Z` form first (RFC 3339 — what
 * `GET /conversations` emits), then the no-`Z` form (kept
 * defensive in case a future revision drops the suffix). Returns
 * `null` when the input cannot be parsed; the mapper falls back
 * to `0L` so the row still renders rather than crashing on a
 * backend regression.
 */
private fun parseIsoMillisOrZero(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return runCatching {
        java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            java.util.Locale.US,
        ).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse(value)?.time
    }.getOrNull() ?: runCatching {
        java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            java.util.Locale.US,
        ).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse(value)?.time
    }.getOrNull()
}
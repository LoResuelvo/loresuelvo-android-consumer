package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ChatMessageDto
import com.loresuelvo.consumer.data.api.dto.DiagnosisDto
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.Sender

/**
 * DTO → domain translation for the AI diagnostic chat backend.
 * Stays in `data/` per the AGENTS.md rule: snake_case ↔ camelCase
 * conversion lives in `mapper/`, never in `domain/` or `ui/`.
 *
 * The mapper is intentionally narrow for commit 02-DIA: only
 * the wire fields exercised by the [Diagnosis] aggregate's
 * `conversationId` / `messages` flow are mapped. The wrapped
 * `chatbot.{...}` shape that the webapp tolerates
 * (`infrastructure/repositories/ai-chat-mapper.ts`) lands when
 * the assessment + providers commits (09-DIA → 11-DIA) force us
 * to consume that nested block.
 */
internal fun DiagnosisDto.toDomain(): Diagnosis {
    val messages = messages.map { it.toDomain() }
    return Diagnosis(
        conversationId = id?.toString() ?: conversationId,
        messages = messages,
        recommendations = null,
    )
}

internal fun ChatMessageDto.toDomain(): ChatMessage {
    val sender = when (senderRole.lowercase()) {
        "consumer" -> Sender.Consumer
        "chatbot" -> Sender.Assistant
        // Defensive default: unknown sender roles from a future
        // backend revision render as assistant bubbles. The login
        // attempt still surfaces through the next request and the
        // server response will reconcile the identity.
        else -> Sender.Assistant
    }
    val sentAtEpochMillis = parseIsoMillisOrZero(sentAt)
        ?: parseIsoMillisOrZero(createdOn)
        ?: 0L
    return ChatMessage(
        // Backend issues numeric ids; the UI key path is the
        // string form so `LazyColumn` keys stay stable across
        // optimistic appends (the local `user-<uuid>` ids
        // never collide with backend numeric ids).
        id = id.toString(),
        sender = sender,
        content = content,
        sentAtEpochMillis = sentAtEpochMillis,
    )
}

/**
 * Best-effort ISO-8601 parser. We can't use `java.time.Instant`
 * because `minSdk = 24`; `SimpleDateFormat` is API-1 friendly and
 * sufficient for the backend's `YYYY-MM-DDTHH:MM:SS` shape.
 *
 * Returned value is in epoch millis. `null` when the input can't
 * be parsed — the mapper falls back to `0L` so the UI can still
 * render the bubble with a "synthetic" timestamp rather than
 * crashing on a backend regression.
 */
private fun parseIsoMillisOrZero(value: String?): Long? = value?.let { raw ->
    runCatching {
        java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            java.util.Locale.US,
        ).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse(raw)?.time
    }.getOrNull()
} ?: when {
    value == null -> null
    else -> null
}

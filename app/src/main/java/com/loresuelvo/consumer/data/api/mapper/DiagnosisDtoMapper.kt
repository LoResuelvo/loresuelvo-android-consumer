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
 * The mapper threads `assessment.problem_category.id` into every
 * mapped recommended provider (the AI wire echoes the category
 * name on each provider but not the id; the consumer app keeps
 * the `Provider.categoryId: Int` non-null invariant by reusing
 * the category id the assessment already carried — see
 * `ProviderDto.toDomain(categoryId)`).
 */
internal fun DiagnosisDto.toDomain(): Diagnosis {
    val messages = messages.map { it.toDomain() }
    val problemCategoryId = assessment?.problemCategory?.id
    return Diagnosis(
        conversationId = id?.toString() ?: conversationId,
        messages = messages,
        assessment = assessment?.toDomain(),
        recommendedProviders = recommendedProviders?.map { provider ->
            provider.toDomain(categoryId = problemCategoryId ?: 0)
        },
    )
}

internal fun ChatMessageDto.toDomain(): ChatMessage {
    val sender = when (senderRole.lowercase()) {
        "consumer" -> Sender.Consumer
        "chatbot" -> Sender.Assistant
        else -> Sender.Assistant
    }
    val sentAtEpochMillis = parseIsoMillisOrZero(sentAt)
        ?: parseIsoMillisOrZero(createdOn)
        ?: 0L
    return ChatMessage(
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
}

package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.AiConversationSummaryDto
import com.loresuelvo.consumer.domain.assistant.AiConversationSummary

/**
 * DTO → domain translation for the AI diagnostic conversation
 * list. The wire shape (`id: Long`, `updated_on: String ISO`,
 * `last_message: ChatMessageDto`) is mapped to the domain's
 * stable-string-id and epoch-millis conventions so the UI can
 * use the result as `LazyColumn` keys without overflow concerns
 * and the timestamp as a `Long` for `Date.from(...)`.
 *
 * `last_message` is optional on the wire (the backend may return
 * a conversation whose only message is the system welcome). When
 * the field is present, the domain flattens it to its `content`
 * string so the list row can render a one-line preview without
 * a second type-level hop.
 */
internal fun AiConversationSummaryDto.toDomain(): AiConversationSummary =
    AiConversationSummary(
        id = id.toString(),
        title = title,
        lastMessageAtEpochMillis = parseIsoTimestampMillisOrZero(updatedOn) ?: 0L,
        lastMessagePreview = lastMessage?.content,
    )

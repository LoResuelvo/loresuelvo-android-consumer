package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ConversationCounterpartDto
import com.loresuelvo.consumer.data.api.dto.ConversationDetailDto
import com.loresuelvo.consumer.data.api.dto.ConversationDto
import com.loresuelvo.consumer.data.api.dto.ConversationMessageDto
import com.loresuelvo.consumer.data.api.dto.MessageAudioDto
import com.loresuelvo.consumer.data.api.dto.MessageImageDto
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.MediaReference

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

internal fun ConversationDetailDto.toDomain(): ConversationDetail {
    // The dev backend wraps the counterpart under `work` on the
    // detail endpoint; the list endpoint keeps it at the root.
    // Prefer the nested shape and fall back to the root field so
    // a future shape drift in either direction doesn't crash the
    // mapper.
    val counterpartDto = work?.counterpart ?: counterpart
        ?: error(
            "ConversationDetailDto has neither work.counterpart nor counterpart",
        )
    return ConversationDetail(
        id = id.toString(),
        status = status.toConversationStatus(),
        counterpart = counterpartDto.toDomain(),
        messages = messages.map { it.toDomain() },
        updatedOnEpochMillis = parseIsoMillisOrZero(updatedOn) ?: 0L,
    )
}

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
        // The backend guarantees at most one of `audio` /
        // `video` / non-empty `images` per persisted message
        // (see `internal/domain/conversation/service.go`
        // `sendParticipantMessage`). Audio wins when present so
        // the bubble renders the player; the image / video
        // branches fall through when the message carries an
        // image-only or video-only attachment.
        media = audio?.toMediaReference()
            ?: images.firstOrNull()?.toMediaReference(),
    )
}

/**
 * Wire `audio` block → domain [MediaReference.Audio]. The
 * backend emits `duration_seconds` as a positive whole-second
 * integer (1–300, rounded up); the domain keeps
 * `durationMillis` (matching the local player / preview
 * pipeline) so the bubble counter and the player share a
 * single `Long` field. `* 1000L` is safe — the backend rounds
 * up, so `5.42 s` becomes `6 s → 6000 ms`, which is the
 * rounded-up value the bubble should display.
 */
private fun MessageAudioDto.toMediaReference(): MediaReference =
    MediaReference.Audio(
        id = id,
        url = url,
        mimeType = mimeType,
        originalName = originalName,
        durationMillis = durationSeconds.toLong() * 1000L,
    )

/**
 * Wire image → domain [MediaReference.Image]. The mapper no
 * longer dispatches on `mimeType`: an audio bubble always
 * arrives under the dedicated `audio` block (mapped above) and
 * the `images` block is image-only by the backend contract
 * (`conversationMessageImagePolicy.AllowedMimeTypes` is
 * `image/{jpeg,png,webp}`). Carrying the mime through
 * unchanged keeps the bubble's mime-aware rendering honest.
 */
private fun MessageImageDto.toMediaReference(): MediaReference =
    MediaReference.Image(
        url = url,
        mimeType = mimeType,
        originalName = originalName,
    )

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
 * `YYYY-MM-DDTHH:MM:SS[.SSSSSS][Z]` shape. We can't use
 * `java.time.Instant` because `minSdk = 24`; `SimpleDateFormat`
 * is API-1 friendly and sufficient for the conversations wire.
 *
 * The dev backend emits microseconds + trailing `Z` (e.g.
 * `2026-08-07T15:46:40.928659Z`); an older endpoint revision
 * emits bare seconds. Tries each shape in turn; the mapper falls
 * back to `0L` when nothing parses so the row still renders
 * rather than crashing on a backend regression.
 */
private fun parseIsoMillisOrZero(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    // `SimpleDateFormat` is lenient on trailing characters by
    // default; the literal `'Z'` (RFC 822 TZ letter) in the
    // pattern is interpreted as a literal `Z` (not the RFC 822
    // offset). Combined with leniency, the parser accepts both
    // `…Z` and `…+0000` suffixes plus any microsecond tail.
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .parse(value)?.time
        }.getOrNull()
    }
}
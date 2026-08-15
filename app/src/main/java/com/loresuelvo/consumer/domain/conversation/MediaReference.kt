package com.loresuelvo.consumer.domain.conversation

/**
 * Reference to a media file attached to a conversation message.
 * Pure domain: describes the persisted side of an attachment (the
 * remote URL the backend issued, the declared mime type, the
 * original file name the user picked, and — for audio only — its
 * duration). The bytes themselves never live in domain; the
 * upload path carries them through the data layer and discards
 * them after the multipart POST returns.
 *
 * Sealed because the consumer ↔ provider chat is expected to grow
 * into additional media kinds (video, voice-note transcripts,
 * file attachments) and we want each variant to declare its own
 * extra fields without leaking them through the parent. The UI
 * renders an exhaustive `when` over the sealed hierarchy so a
 * new variant never ships unhandled.
 *
 * Naming: `MediaReference` instead of `ConversationMedia` to keep
 * the "what the server persisted" semantics crystal-clear. The
 * upload counterpart (the byte payload the consumer is about to
 * send) lives in [MediaUpload] — split so domain callers can
 * carry either kind without confusing the two.
 */
sealed interface MediaReference {

    val url: String

    val mimeType: String

    val originalName: String

    /**
     * Image attachment. Carries no extra fields beyond the common
     * [url] / [mimeType] / [originalName] — the backend stores
     * dimensions as part of the URL response, not the wire
     * envelope.
     */
    data class Image(
        override val url: String,
        override val mimeType: String,
        override val originalName: String,
    ) : MediaReference

    /**
     * Audio attachment. Carries the recording's duration in
     * milliseconds so the chat bubble can render a `mm:ss`
     * counter without a second round-trip to fetch it.
     */
    data class Audio(
        override val url: String,
        override val mimeType: String,
        override val originalName: String,
        val durationMillis: Long,
    ) : MediaReference
}
package com.loresuelvo.consumer.domain.conversation

/**
 * Single message in a consumer ↔ provider conversation.
 *
 *  - [id] is the backend-issued numeric id (stringified so the
 *    `LazyColumn` key path stays a stable [String], mirroring the
 *    AI diagnostic `ChatMessage.id` convention).
 *  - [sender] discriminates between the consumer's bubble and the
 *    provider's bubble. Distinct from `domain.diagnosis.Sender`
 *    because the two domains model different actors: a provider
 *    reply is conceptually different from an AI assistant reply,
 *    and forcing the same sealed type would let a future
 *    `Sender.Assistant` bubble leak into the provider UI by
 *    accident.
 *  - [content] is the message body in plain text. Empty string
 *    when the message is media-only (the user picked a photo and
 *    sent it without a caption).
 *  - [media] is the optional media attachment the message
 *    carries. `null` for text-only messages. A bubble can be
 *    both: a short caption with an image attached, modelled as a
 *    non-empty [content] AND a non-null [media]. The UI decides
 *    how to render each combination.
 *  - [createdOnEpochMillis] is the backend's `created_on` parsed
 *    to epoch millis (UTC). `Long` (not `java.time.Instant`)
 *    because `minSdk = 24`.
 *
 * Pure domain.
 */
data class ConversationMessage(
    val id: String,
    val sender: ConversationSender,
    val content: String,
    val createdOnEpochMillis: Long,
    val media: MediaReference? = null,
)
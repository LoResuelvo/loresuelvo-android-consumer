package com.loresuelvo.consumer.domain.diagnosis

/**
 * Aggregate returned by the AI diagnosis backend after the consumer
 * sends a message. Carries:
 *
 *  - [conversationId]: the backend-issued conversation id; `null`
 *    only when the optimistic local message has not yet been
 *    confirmed (and even then, [DiagnosisRepository.sendPrompt] will
 *    keep returning the same conversation on subsequent calls).
 *  - [messages]: full ordered history (consumer → assistant).
 *  - [recommendations]: optional assessment + providers block,
 *    populated when the assistant concludes the diagnosis. Lands
 *    alongside the LAST assistant message at the UI layer. Future
 *    commits flesh this out (see scenarios 09-DIA → 11-DIA).
 *
 * The recommendations field is intentionally nullable so an
 * in-flight conversation can carry `null` without dragging the full
 * assessment / provider taxonomy into commits that don't need it.
 */
data class Diagnosis(
    val conversationId: String?,
    val messages: List<ChatMessage>,
    val recommendations: Recommendations? = null,
)

/**
 * Diagnosis outcome attached to the conversation when the assistant
 * concludes. `null` while the AI is still collecting information.
 *
 * Reserved for commits 09-DIA → 11-DIA. For now an empty data
 * class so the [Diagnosis] aggregate can carry the field without
 * forcing the conversations that don't have recommendations yet to
 * materialise the full Assessment / RecommendedProvider taxonomy.
 */
data class Recommendations(
    val stub: Boolean = true,
)

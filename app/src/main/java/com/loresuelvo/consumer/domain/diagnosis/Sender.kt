package com.loresuelvo.consumer.domain.diagnosis

/**
 * Identifies who produced a [ChatMessage]. Modeled as a sealed
 * interface (rather than an enum) so future subtypes can carry their
 * own state without touching consumers — e.g. an assistant message
 * that references a previous diagnostic assessment.
 *
 * The webapp calls this `senderRole: "consumer" | "chatbot"`. We
 * keep the same two cases but expose them with a single
 * discriminator (`Sender`) so the UI's `when` branches stay
 * exhaustive.
 */
sealed interface Sender {
    data object Consumer : Sender
    data object Assistant : Sender
}

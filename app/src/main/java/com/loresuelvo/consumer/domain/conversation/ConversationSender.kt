package com.loresuelvo.consumer.domain.conversation

/**
 * Identifies who produced a [ConversationMessage]. Modeled as a
 * sealed interface (rather than an enum) so future subtypes can
 * carry their own state without touching consumers — e.g. a
 * `System` sender for "Provider accepted the conversation" events.
 *
 * Distinct from `domain.diagnosis.Sender` because that one models
 * the AI diagnostic (`Consumer` / `Assistant`), and the provider's
 * role in a 1:1 chat is conceptually different.
 *
 * Pure domain.
 */
sealed interface ConversationSender {
    data object Consumer : ConversationSender
    data object Provider : ConversationSender
}
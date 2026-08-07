package com.loresuelvo.consumer.domain.conversation

/**
 * Lifecycle status of a [Conversation] from the consumer's
 * perspective. Modeled as a sealed interface with a single
 * known value today ([Pending]) and an [Other] fallback for any
 * status the backend may introduce later without forcing an app
 * release.
 *
 * Wire translation lives in
 * `data/api/mapper/ConversationDtoMapper.kt` (where the snake_case
 * string from the backend becomes one of these variants). The
 * domain never sees the raw string.
 *
 * Pure domain.
 */
sealed interface ConversationStatus {
    /**
     * The consumer has sent the first message; the provider has
     * not accepted the conversation yet. The list cell renders a
     * "Pendiente de aceptación" badge for this status.
     */
    data object Pending : ConversationStatus

    /**
     * Forward-compatible bucket for any value the backend may add
     * (`accepted`, `rejected`, `closed`, …). The UI renders the
     * raw string verbatim so the cell never crashes on a backend
     * revision.
     */
    data class Other(val raw: String) : ConversationStatus
}
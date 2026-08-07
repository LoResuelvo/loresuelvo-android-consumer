package com.loresuelvo.consumer.domain.conversation

/**
 * Port for the consumer ↔ provider conversations backend.
 * Implementations live in `data/api/` and translate the wire
 * contract (snake_case JSON over the Retrofit-typed
 * [com.loresuelvo.consumer.data.api.BackendApi]) into the
 * domain's [ConversationsOutcome] hierarchy.
 *
 * The repository never throws on HTTP / network failures: every
 * exception is mapped to a typed
 * [ConversationsOutcome.Failure] (exhaustive `when` against
 * [com.loresuelvo.consumer.domain.api.ApiError]).
 * Implementations must remain pure with respect to UI concerns —
 * no Android, no kotlinx-serialization, no Hilt.
 */
interface ConversationRepository {

    /**
     * Lists every conversation the authenticated consumer has
     * opened, ordered by the backend's `updated_on` descending
     * (most recent first). Each entry includes the counterpart
     * (provider) profile and the last message preview.
     *
     * Empty list ⇒ the consumer has not started any conversation
     * yet; the UI renders an "empty state" CTA.
     */
    suspend fun getConversations(): ConversationsOutcome
}
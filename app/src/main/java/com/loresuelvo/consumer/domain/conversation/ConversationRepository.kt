package com.loresuelvo.consumer.domain.conversation

/**
 * Port for the consumer ↔ provider conversations backend.
 * Implementations live in `data/api/` and translate the wire
 * contract (snake_case JSON over the Retrofit-typed
 * [com.loresuelvo.consumer.data.api.BackendApi]) into the
 * domain's [ConversationsOutcome] hierarchy.
 *
 * The repository never throws on HTTP / network failures: every
 * exception is mapped to a typed `Failure` (exhaustive `when`
 * against [com.loresuelvo.consumer.domain.api.ApiError]).
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

    /**
     * Loads the full snapshot of a single conversation,
     * including the complete ordered message thread. The chat
     * surface uses it on entry to render the header (counterpart,
     * status) and the existing bubbles. Empty `messages` is a
     * valid response for a brand-new conversation that the
     * consumer has just opened.
     */
    suspend fun getConversationById(
        conversationId: String,
    ): ConversationDetailOutcome

    /**
     * Appends a consumer-typed message to the given conversation.
     * The [content] is sent verbatim to the backend; trimming /
     * blank-guarding is the use case's responsibility.
     *
     * On success the carried [ConversationMessage] is the
     * server-persisted bubble (with the backend's stable id and
     * authoritative timestamp). The VM uses it to replace the
     * optimistic bubble it appended locally.
     *
     * 404 on `conversationId` (the provider or consumer dropped
     * the thread) maps to [SendMessageOutcome.Failure.Server].
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String,
    ): SendMessageOutcome
}
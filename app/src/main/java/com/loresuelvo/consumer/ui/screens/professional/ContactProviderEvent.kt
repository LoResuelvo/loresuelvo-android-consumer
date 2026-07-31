package com.loresuelvo.consumer.ui.screens.professional

/**
 * One-shot side effects emitted by [ContactProviderViewModel] and
 * consumed by the navigation host in `LoResuelvoNav`. Reflects
 * the user's "navigate directly to the chat" requirement (no
 * intermediate screens).
 */
sealed interface ContactProviderEvent {

    /**
     * The `POST /job-requests` round-trip succeeded and the
     * backend returned a `conversation_id`. The host navigates
     * to `Route.Conversation(conversationId)`. The full chat
     * surface is a placeholder for now — the actual messages
     * UI is fleshed out in a follow-up US.
     */
    data class NavigateToConversation(val conversationId: String) : ContactProviderEvent
}

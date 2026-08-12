package com.loresuelvo.consumer.ui.screens.chat

/**
 * One-shot side effects emitted by [AiDiagnosisContactViewModel]
 * and consumed by the navigation host in `ChatRoute`. Mirrors
 * the convention used by `ContactProviderEvent` for the
 * Professionals flow, but the AI pre-fill variant only carries
 * `NavigateToConversation` — the AI fills `title` and
 * `description` server-side, so the UI does NOT navigate to a
 * modal form. The chat route pops to `Route.Conversation` and
 * lands the user on the conversation with the provider.
 */
sealed interface AiDiagnosisContactEvent {

    /**
     * The `POST /chatbot/conversations/{conversationId}/job-requests`
     * round-trip succeeded and the backend returned a
     * `conversation_id` (the AI-generated job request already
     * attached the first message). The host navigates to
     * `Route.Conversation(conversationId)`.
     */
    data class NavigateToConversation(val conversationId: String) : AiDiagnosisContactEvent
}

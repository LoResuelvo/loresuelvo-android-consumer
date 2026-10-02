package com.loresuelvo.consumer.ui.screens.professional

/**
 * One-shot side effects emitted by [ContactProviderViewModel] and
 * consumed by the navigation host in `LoResuelvoNav`. Reflects
 * the user's "navigate directly to the chat" requirement (no
 * intermediate screens).
 */
sealed interface ContactProviderEvent {

    /** The job request succeeded and navigation can open the conversation. */
    data class NavigateToConversation(val conversationId: String) : ContactProviderEvent
}

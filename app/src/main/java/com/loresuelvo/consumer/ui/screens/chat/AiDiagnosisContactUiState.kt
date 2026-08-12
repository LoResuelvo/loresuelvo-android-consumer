package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.provider.Provider

/**
 * UDF state for the AI pre-filled contact flow.
 *
 *  - [Idle]: the chat carousel has never been tapped, or the
 *    previous round-trip finished. The carousel renders normally.
 *  - [Submitting]: the user tapped "Contactar" on [provider] and
 *    the `POST /chatbot/conversations/{id}/job-requests` round-trip
 *    is still in flight. The carousel button is disabled via
 *    `ChatRoute` as a defensive guard so the user cannot stack
 *    in-flight requests on the backend.
 *
 * Errors from the round-trip are NOT surfaced as a typed UI state
 * today — the chat surface keeps its current state and the user can
 * tap again. A future ticket can promote [Failure] to a typed
 * failure UI (toast / inline error) once the BDD scenarios pin it.
 */
sealed interface AiDiagnosisContactUiState {

    data object Idle : AiDiagnosisContactUiState

    data class Submitting(val provider: Provider) : AiDiagnosisContactUiState
}

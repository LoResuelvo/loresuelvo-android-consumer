package com.loresuelvo.consumer.domain.assistant

/**
 * Port for the AI diagnostic conversation list surfaced in the
 * "Asistente IA" tab. Mirrors the convention used by the rest of
 * the AI diagnostic surface (`DiagnosisRepository` owns the chat
 * thread ops, `AiJobRequestRepository` owns the AI pre-filled
 * job-request creation, this owns the list of past sessions).
 *
 * Implemented in `data/api/ApiAiConversationRepository.kt`.
 * Implementations must NOT throw on HTTP / network failures; they
 * translate to a typed [AiConversationListOutcome.Failure].
 */
interface AiConversationRepository {

    /**
     * `GET /chatbot/conversations` — the consumer's AI diagnostic
     * conversations, ordered by the backend's `updated_on`
     * policy (currently timestamp-descending). Each entry is
     * the row-level summary the "Asistente IA" tab renders; the
     * full chat thread (messages + assessment + providers) is
     * fetched on tap via `DiagnosisRepository` (the assistant
     * list and the chat thread are intentionally separate
     * endpoints so the list payload stays small).
     */
    suspend fun getConversations(): AiConversationListOutcome
}

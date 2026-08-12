package com.loresuelvo.consumer.domain.jobrequest

/**
 * Port for the AI pre-filled job-request creation flow. Distinct
 * from [JobRequestRepository]: the OLD flow (`POST /job-requests`)
 * requires the consumer to type a title and description in the
 * modal form, while THIS flow (`POST /chatbot/conversations/{id}/job-requests`)
 * is fired when the consumer taps "Contactar" on a recommended
 * provider tile INSIDE the AI diagnostic chat. The backend's AI
 * pre-fills `title` and `description` from the conversation
 * context (mirrors the webapp's `useAiDiagnosisChat.handleContactProvider`).
 *
 * Implemented in `data/api/ApiAiJobRequestRepository.kt`.
 * Implementations must NOT throw on HTTP / network failures; they
 * translate to a typed [CreateAiJobRequestOutcome.Failure].
 */
interface AiJobRequestRepository {

    /**
     * Submits the AI pre-filled job request to the provider
     * identified by [providerId]. The backend returns the
     * persisted [JobRequest] including the backend-issued `id`
     * and the `conversationId` the UI navigates to on success.
     */
    suspend fun createAiJobRequest(
        conversationId: String,
        providerId: Int,
    ): CreateAiJobRequestOutcome
}

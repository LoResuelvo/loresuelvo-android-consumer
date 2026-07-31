package com.loresuelvo.consumer.domain.jobrequest

/**
 * Port for the job request lifecycle. Implemented in
 * `data/api/ApiJobRequestRepository.kt` against the backend's
 * `POST /job-requests` endpoint. No Android, no Retrofit, no
 * JSON — just the contract the use cases depend on.
 *
 * Implementations must NOT throw on HTTP / network failures; they
 * translate to a typed [CreateJobRequestOutcome.Failure].
 */
interface JobRequestRepository {

    /**
     * Submits the first message of a conversation with [providerId].
     * The backend returns the persisted [JobRequest] including
     * the backend-issued `id` and the conversation id that the
     * UI navigates to on success.
     */
    suspend fun createJobRequest(data: CreateJobRequestData): CreateJobRequestOutcome
}

package com.loresuelvo.consumer.domain.usecase.jobrequest

import com.loresuelvo.consumer.domain.jobrequest.AiJobRequestRepository
import com.loresuelvo.consumer.domain.jobrequest.CreateAiJobRequestOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Submits the AI pre-filled job request to the backend after
 * enforcing the minimum business rules the backend validates
 * server-side:
 *
 *  - `conversationId` is non-blank — the backend rejects empty
 *    values; the use case short-circuits with a typed
 *    [CreateAiJobRequestOutcome.Failure.Server] (`code = 0`,
 *    synthetic non-HTTP) before the round-trip, mirroring
 *    `CreateJobRequestUseCase`'s "title is required" guard.
 *  - `providerId > 0` — defensive only; the AI chat only carries
 *    valid providers from the backend's `recommended_providers[]`
 *    list, so this guard is a no-op in practice but it documents
 *    the contract.
 *  - The repository's typed failures (Network / Server /
 *    Unauthorized) propagate unchanged.
 */
@Singleton
class CreateAiJobRequestUseCase @Inject constructor(
    private val repository: AiJobRequestRepository,
) {
    suspend operator fun invoke(
        conversationId: String,
        providerId: Int,
    ): CreateAiJobRequestOutcome {
        if (conversationId.isBlank()) {
            return CreateAiJobRequestOutcome.Failure.Server(
                code = 0,
                message = "Conversation id is required",
            )
        }
        if (providerId <= 0) {
            return CreateAiJobRequestOutcome.Failure.Server(
                code = 0,
                message = "Provider id is required",
            )
        }
        return repository.createAiJobRequest(
            conversationId = conversationId,
            providerId = providerId,
        )
    }
}

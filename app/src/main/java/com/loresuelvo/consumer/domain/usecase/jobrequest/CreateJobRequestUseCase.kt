package com.loresuelvo.consumer.domain.usecase.jobrequest

import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Submits a job request to the backend after enforcing the same
 * business rules the backend validates server-side:
 *
 *  - `title` and `description` are trimmed before the request
 *    goes out — the backend rejects empty values after trimming.
 *  - A blank title or description is surfaced as a typed
 *    [CreateJobRequestOutcome.Failure.Server] with `code = 0`
 *    (synthetic, non-HTTP). Mirrors the pattern used by
 *    `RegisterConsumerUseCase` for the missing-email guard.
 *  - The repository's typed failures (Network / Server / Unauthorized)
 *    propagate unchanged.
 *
 * The use case is the only place where the form data is normalised
 * before the wire round-trip. The UI's "Enviar solicitud" button
 * is gated on non-empty fields, so the client-side guards here
 * are a defensive mirror — useful for tests and any future
 * programmatic callers.
 */
@Singleton
class CreateJobRequestUseCase @Inject constructor(
    private val jobRequestRepository: JobRequestRepository,
) {
    suspend operator fun invoke(data: CreateJobRequestData): CreateJobRequestOutcome {
        val trimmedTitle = data.title.trim()
        val trimmedDescription = data.description.trim()
        if (trimmedTitle.isEmpty()) {
            return CreateJobRequestOutcome.Failure.Server(
                code = 0,
                message = "Title is required",
            )
        }
        if (trimmedDescription.isEmpty()) {
            return CreateJobRequestOutcome.Failure.Server(
                code = 0,
                message = "Description is required",
            )
        }
        return jobRequestRepository.createJobRequest(
            data.copy(
                title = trimmedTitle,
                description = trimmedDescription,
            ),
        )
    }
}

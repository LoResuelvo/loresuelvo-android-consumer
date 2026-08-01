package com.loresuelvo.consumer.bdd.providers.contact

import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequest
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory [JobRequestRepository] used by the contact-provider BDD
 * layer. Mirrors the pattern of `FakeUserRepository` and
 * `FakeDiagnosisRepository`: the BDD enqueues an outcome per
 * scenario, and the VM's `CreateJobRequestUseCase` consumes it on
 * the first `createJobRequest(...)` call.
 *
 * Three enqueue modes are exposed:
 *
 *  - [enqueueOutcome] — returns the given outcome on the next
 *    call (happy path used by 02-SRP).
 *  - [enqueueSuccess] — convenience that builds a `Success`
 *    with a configurable `conversationId` so the BDD can assert
 *    on the `NavigateToConversation` event payload.
 *  - [enqueueFailure] — convenience for typed failures (Network /
 *    Server / Unauthorized).
 *
 * Mirrors the discipline of `FakeDiagnosisRepository`:
 * `lastData` is captured so the BDD can verify the form sends the
 * expected provider id / title / description.
 */
class FakeJobRequestRepository : JobRequestRepository {

    private val nextOutcome = AtomicReference<CreateJobRequestOutcome?>(null)

    /**
     * The payload the VM forwarded to the repository on the last
     * call. Read-only so the BDD can assert on the wire data
     * shape (provider id, title, description) without driving a
     * stubbed HTTP layer.
     */
    var lastData: CreateJobRequestData? = null
        private set

    fun enqueueOutcome(outcome: CreateJobRequestOutcome) {
        nextOutcome.set(outcome)
    }

    fun enqueueSuccess(conversationId: String = "fake-conv-1") {
        nextOutcome.set(
            CreateJobRequestOutcome.Success(
                JobRequest(
                    id = "fake-job-1",
                    conversationId = conversationId,
                    title = STUB_FIELD,
                    description = STUB_FIELD,
                    status = "pending",
                    images = emptyList(),
                ),
            ),
        )
    }

    fun enqueueFailure(failure: CreateJobRequestOutcome.Failure) {
        nextOutcome.set(failure)
    }

    override suspend fun createJobRequest(data: CreateJobRequestData): CreateJobRequestOutcome {
        lastData = data
        val queued = nextOutcome.getAndSet(null)
        if (queued != null) return queued
        // Default success when no outcome was enqueued — keeps the
        // BDD scenarios terse: callers only enqueue when they want
        // to force a specific failure path.
        return CreateJobRequestOutcome.Success(
            JobRequest(
                id = "fake-job-1",
                conversationId = "fake-conv-1",
                title = data.title,
                description = data.description,
                status = "pending",
                images = emptyList(),
            ),
        )
    }

    private companion object {
        const val STUB_FIELD = "irrelevant"
    }
}

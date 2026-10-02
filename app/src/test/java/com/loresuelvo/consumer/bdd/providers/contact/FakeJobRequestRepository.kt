package com.loresuelvo.consumer.bdd.providers.contact

import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequest
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import java.util.concurrent.atomic.AtomicReference

class FakeJobRequestRepository : JobRequestRepository {

    private val nextOutcome = AtomicReference<CreateJobRequestOutcome?>(null)

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

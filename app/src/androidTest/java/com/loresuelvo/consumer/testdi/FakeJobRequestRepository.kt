package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequest
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeJobRequestRepository @Inject constructor() : JobRequestRepository {
    override suspend fun createJobRequest(data: CreateJobRequestData): CreateJobRequestOutcome {
        val jr = JobRequest(
            id = "fake-job-1",
            conversationId = "fake-conv-1",
            title = data.title,
            description = data.description,
            status = "pending",
            images = emptyList(),
        )
        return CreateJobRequestOutcome.Success(jr)
    }
}

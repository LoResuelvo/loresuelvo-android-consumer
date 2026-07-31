package com.loresuelvo.consumer.domain.usecase.jobrequest

import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequest
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the contract of [CreateJobRequestUseCase]:
 *  - trims whitespace before forwarding to the repository;
 *  - short-circuits with a [CreateJobRequestOutcome.Failure.Server]
 *    when the title or description is blank after trimming
 *    (matching the backend's `POST /job-requests` validation);
 *  - propagates the repository's typed failures unchanged.
 *
 * Mirrors the convention used by `RegisterConsumerUseCaseTest`.
 */
class CreateJobRequestUseCaseTest {

    private val repository: JobRequestRepository = mockk()
    private val useCase = CreateJobRequestUseCase(repository)

    @Test
    fun `forwards a valid data to the repository and returns its success`() = runTest {
        val data = CreateJobRequestData(
            providerId = 1,
            title = "Fuga en la cocina",
            description = "Necesito reparar una fuga",
        )
        val expected = JobRequest(
            id = "1",
            conversationId = "10",
            title = "Fuga en la cocina",
            description = "Necesito reparar una fuga",
            status = "pending",
            images = emptyList(),
        )
        coEvery { repository.createJobRequest(any()) } returns
            CreateJobRequestOutcome.Success(expected)

        val outcome = useCase(data)

        assertTrue(outcome is CreateJobRequestOutcome.Success)
        assertEquals(expected, (outcome as CreateJobRequestOutcome.Success).jobRequest)
    }

    @Test
    fun `trims leading and trailing whitespace from title and description before forwarding`() = runTest {
        val captured = slot<CreateJobRequestData>()
        coEvery { repository.createJobRequest(capture(captured)) } returns
            CreateJobRequestOutcome.Success(
                JobRequest(
                    id = "1",
                    conversationId = "10",
                    title = "Fuga",
                    description = "Hay una fuga",
                    status = "pending",
                    images = emptyList(),
                ),
            )

        useCase(
            CreateJobRequestData(
                providerId = 1,
                title = "  Fuga  ",
                description = "  Hay una fuga  ",
            ),
        )

        assertEquals("Fuga", captured.captured.title)
        assertEquals("Hay una fuga", captured.captured.description)
    }

    @Test
    fun `returns Server failure when title is blank after trim`() = runTest {
        val outcome = useCase(
            CreateJobRequestData(
                providerId = 1,
                title = "   ",
                description = "Description",
            ),
        )

        assertTrue(outcome is CreateJobRequestOutcome.Failure.Server)
        assertEquals(0, (outcome as CreateJobRequestOutcome.Failure.Server).code)
        // The repository must NOT be called when the title is blank:
        // the guard short-circuits before the HTTP round-trip.
        coVerify(exactly = 0) { repository.createJobRequest(any()) }
    }

    @Test
    fun `returns Server failure when description is blank after trim`() = runTest {
        val outcome = useCase(
            CreateJobRequestData(
                providerId = 1,
                title = "Title",
                description = "   ",
            ),
        )

        assertTrue(outcome is CreateJobRequestOutcome.Failure.Server)
        assertEquals(0, (outcome as CreateJobRequestOutcome.Failure.Server).code)
        coVerify(exactly = 0) { repository.createJobRequest(any()) }
    }

    @Test
    fun `propagates the repository's failure unchanged`() = runTest {
        val failure = CreateJobRequestOutcome.Failure.Server(500, "boom")
        coEvery { repository.createJobRequest(any()) } returns failure

        val outcome = useCase(
            CreateJobRequestData(
                providerId = 1,
                title = "Title",
                description = "Description",
            ),
        )

        assertEquals(failure, outcome)
    }

    @Test
    fun `propagates Network failure unchanged`() = runTest {
        val failure = CreateJobRequestOutcome.Failure.Network(IllegalStateException("socket closed"))
        coEvery { repository.createJobRequest(any()) } returns failure

        val outcome = useCase(
            CreateJobRequestData(
                providerId = 1,
                title = "Title",
                description = "Description",
            ),
        )

        assertEquals(failure, outcome)
    }
}

package com.loresuelvo.consumer.domain.usecase.workorder

import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import com.loresuelvo.consumer.domain.workorder.WorkOrderReview
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RateProviderUseCaseTest {

    private fun savedReview(rating: Int = 5, description: String = "Excelente trabajo"): WorkOrderReview =
        WorkOrderReview(rating = rating, description = description)

    @Test
    fun returns_Submitted_when_repository_accepts_the_review() = runTest {
        val review = savedReview()
        val fake = FakeRepo(nextOutcome = SubmitWorkOrderReviewOutcome.Submitted(review))
        val useCase = RateProviderUseCase(fake)

        val outcome = useCase(workOrderId = "wo-100", rating = 5, description = "Excelente trabajo")

        assertTrue(
            "expected Submitted, was $outcome",
            outcome is SubmitWorkOrderReviewOutcome.Submitted,
        )
        assertSame(review, (outcome as SubmitWorkOrderReviewOutcome.Submitted).review)
        // The fake recorded the exact parameters forwarded by the use case.
        assertEquals("wo-100", fake.lastWorkOrderId)
        assertEquals(5, fake.lastRating)
        assertEquals("Excelente trabajo", fake.lastDescription)
    }

    @Test
    fun returns_Network_when_repository_surfaces_a_transport_failure() = runTest {
        val cause = IOException("connection refused")
        val fake = FakeRepo(nextOutcome = SubmitWorkOrderReviewOutcome.Network(cause))
        val useCase = RateProviderUseCase(fake)

        val outcome = useCase("wo-100", 4, "")

        assertTrue(
            "expected Network, was $outcome",
            outcome is SubmitWorkOrderReviewOutcome.Network,
        )
        assertSame(cause, (outcome as SubmitWorkOrderReviewOutcome.Network).cause)
    }

    @Test
    fun returns_Server_when_repository_surfaces_a_non_2xx_response() = runTest {
        val failure = SubmitWorkOrderReviewOutcome.Server(code = 500, message = "down for maintenance")
        val fake = FakeRepo(nextOutcome = failure)
        val useCase = RateProviderUseCase(fake)

        val outcome = useCase("wo-100", 3, "ok")

        assertTrue(
            "expected Server, was $outcome",
            outcome is SubmitWorkOrderReviewOutcome.Server,
        )
        val server = outcome as SubmitWorkOrderReviewOutcome.Server
        assertEquals(500, server.code)
        assertEquals("down for maintenance", server.message)
    }

    @Test
    fun returns_AlreadyReviewed_when_repository_surfaces_a_409_conflict() = runTest {
        val failure = SubmitWorkOrderReviewOutcome.AlreadyReviewed(message = "ya calificaste esta orden")
        val fake = FakeRepo(nextOutcome = failure)
        val useCase = RateProviderUseCase(fake)

        val outcome = useCase("wo-100", 5, "Bien")

        assertTrue(
            "expected AlreadyReviewed, was $outcome",
            outcome is SubmitWorkOrderReviewOutcome.AlreadyReviewed,
        )
        assertEquals(
            "ya calificaste esta orden",
            (outcome as SubmitWorkOrderReviewOutcome.AlreadyReviewed).message,
        )
    }

    @Test
    fun forwards_blank_description_unchanged() = runTest {
        val fake = FakeRepo(
            nextOutcome = SubmitWorkOrderReviewOutcome.Submitted(savedReview(rating = 4, description = "")),
        )
        val useCase = RateProviderUseCase(fake)

        useCase("wo-100", 4, "")

        assertEquals("", fake.lastDescription)
    }

    /**
     * Port-level fake. Records the last submission payload and
     * returns the queued outcome as-is; this lets the test
     * assert both the typed outcome and that the use case
     * forwarded the parameters to the repository untouched.
     */
    private class FakeRepo(
        private val nextOutcome: SubmitWorkOrderReviewOutcome,
    ) : WorkOrderDetailRepository {

        var lastWorkOrderId: String? = null
            private set
        var lastRating: Int? = null
            private set
        var lastDescription: String? = null
            private set

        override suspend fun getWorkOrderDetail(
            workOrderId: String,
            provider: com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart?,
        ): GetWorkOrderOutcome =
            error("getWorkOrderDetail is not exercised by RateProviderUseCaseTest")

        override suspend fun submitReview(
            workOrderId: String,
            rating: Int,
            description: String,
        ): SubmitWorkOrderReviewOutcome {
            lastWorkOrderId = workOrderId
            lastRating = rating
            lastDescription = description
            return nextOutcome
        }
    }
}

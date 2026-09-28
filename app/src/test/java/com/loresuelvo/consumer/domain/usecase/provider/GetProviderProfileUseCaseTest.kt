package com.loresuelvo.consumer.domain.usecase.provider

import com.loresuelvo.consumer.domain.provider.ProviderCategory
import com.loresuelvo.consumer.domain.provider.ProviderProfile
import com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome
import com.loresuelvo.consumer.domain.provider.ProviderProfileRepository
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrder
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrderStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetProviderProfileUseCaseTest {

    @Test
    fun returns_completed_work_orders_newest_first_and_drops_non_completed_orders() = runTest {
        val profile = profile(
            workOrders = listOf(
                workOrder("scheduled", ProviderWorkOrderStatus.Scheduled, 100L),
                workOrder("old", ProviderWorkOrderStatus.Paid, 1_000L),
                workOrder("new", ProviderWorkOrderStatus.AwaitingPayment, 2_000L),
                workOrder("cancelled", ProviderWorkOrderStatus.Cancelled, 3_000L),
            ),
        )

        val outcome = GetProviderProfileUseCase(FakeRepository(profile))(12)

        assertTrue(outcome is ProviderProfileOutcome.Success)
        assertEquals(
            listOf("new", "old"),
            (outcome as ProviderProfileOutcome.Success).profile.workOrders.map { it.id },
        )
    }

    @Test
    fun preserves_zero_reputation_and_empty_history() = runTest {
        val outcome = GetProviderProfileUseCase(
            FakeRepository(profile(ratingAverage = 0.0, ratingCount = 0, workOrders = emptyList())),
        )(12)

        assertTrue(outcome is ProviderProfileOutcome.Success)
        val result = (outcome as ProviderProfileOutcome.Success).profile
        assertEquals(0.0, result.ratingAverage, 0.0)
        assertEquals(0, result.ratingCount)
        assertTrue(result.workOrders.isEmpty())
    }

    private fun profile(
        ratingAverage: Double = 4.5,
        ratingCount: Int = 2,
        workOrders: List<ProviderWorkOrder>,
    ) = ProviderProfile(
        id = 12,
        name = "Juan",
        surname = "Gómez",
        profilePhotoUrl = null,
        category = ProviderCategory(1, "Plomería"),
        ratingAverage = ratingAverage,
        ratingCount = ratingCount,
        identityVerified = true,
        workOrders = workOrders,
    )

    private fun workOrder(
        id: String,
        status: ProviderWorkOrderStatus,
        scheduledOn: Long,
    ) = ProviderWorkOrder(
        id = id,
        scheduledOnEpochMillis = scheduledOn,
        description = "Trabajo",
        status = status,
        completionReport = null,
        review = null,
    )

    private class FakeRepository(
        private val profile: ProviderProfile,
    ) : ProviderProfileRepository {
        override suspend fun getProviderProfile(providerId: Int): ProviderProfileOutcome =
            ProviderProfileOutcome.Success(profile)
    }
}

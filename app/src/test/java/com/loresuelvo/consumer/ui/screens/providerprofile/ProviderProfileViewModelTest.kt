package com.loresuelvo.consumer.ui.screens.providerprofile

import com.loresuelvo.consumer.domain.provider.ProviderCategory
import com.loresuelvo.consumer.domain.provider.ProviderProfile
import com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome
import com.loresuelvo.consumer.domain.provider.ProviderProfileRepository
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrder
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrderStatus
import com.loresuelvo.consumer.domain.usecase.provider.GetProviderProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProviderProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_success_exposes_profile_and_history() = runTest(dispatcher) {
        val profile = sampleProfile()
        val viewModel = ProviderProfileViewModel(
            GetProviderProfileUseCase(FakeRepository(ProviderProfileOutcome.Success(profile))),
        )

        viewModel.load(12)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProviderProfileUiState.Ready)
        assertEquals("Juan", (state as ProviderProfileUiState.Ready).profile.name)
        assertEquals(1, state.profile.workOrders.size)
    }

    @Test
    fun load_failure_exposes_typed_failure() = runTest(dispatcher) {
        val failure = ProviderProfileOutcome.Failure.Server(503, "unavailable")
        val viewModel = ProviderProfileViewModel(
            GetProviderProfileUseCase(FakeRepository(failure)),
        )

        viewModel.load(12)
        advanceUntilIdle()

        assertEquals(ProviderProfileUiState.Error(failure), viewModel.uiState.value)
    }

    private fun sampleProfile() = ProviderProfile(
        id = 12,
        name = "Juan",
        surname = "Gómez",
        profilePhotoUrl = null,
        category = ProviderCategory(1, "Plomería"),
        ratingAverage = 4.5,
        ratingCount = 2,
        identityVerified = true,
        workOrders = listOf(
            ProviderWorkOrder(
                id = "84",
                scheduledOnEpochMillis = 1_755_270_000_000,
                description = "Reparación de pérdida",
                status = ProviderWorkOrderStatus.Paid,
                completionReport = null,
                review = null,
            ),
        ),
    )

    private class FakeRepository(
        private val outcome: ProviderProfileOutcome,
    ) : ProviderProfileRepository {
        override suspend fun getProviderProfile(providerId: Int): ProviderProfileOutcome = outcome
    }
}

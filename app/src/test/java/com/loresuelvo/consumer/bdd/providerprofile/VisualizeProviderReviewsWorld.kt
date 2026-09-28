package com.loresuelvo.consumer.bdd.providerprofile

import com.loresuelvo.consumer.domain.provider.ProviderCategory
import com.loresuelvo.consumer.domain.provider.ProviderProfile
import com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome
import com.loresuelvo.consumer.domain.provider.ProviderProfileRepository
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrder
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrderStatus
import com.loresuelvo.consumer.domain.usecase.provider.GetProviderProfileUseCase
import com.loresuelvo.consumer.ui.screens.providerprofile.ProviderProfileUiState
import com.loresuelvo.consumer.ui.screens.providerprofile.ProviderProfileViewModel
import io.cucumber.java.After
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class VisualizeProviderReviewsWorld {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private lateinit var viewModel: ProviderProfileViewModel
    private var profile = sampleProfile()

    fun startScenario() {
        Dispatchers.setMain(dispatcher)
        viewModel = ProviderProfileViewModel(
            GetProviderProfileUseCase(FakeRepository { profile }),
        )
    }

    fun showRecommendedProviders() {
        profile = sampleProfile()
    }

    fun showCategoryProviders() {
        profile = sampleProfile()
    }

    fun markProviderAsRated() {
        profile = profile.copy(ratingAverage = 4.5, ratingCount = 2)
    }

    fun openProviderProfile() {
        scope.runTest {
            viewModel.load(profile.id)
            advanceUntilIdle()
        }
    }

    fun readyProfile(): ProviderProfile =
        (viewModel.uiState.value as ProviderProfileUiState.Ready).profile

    @After
    fun close() {
        Dispatchers.resetMain()
    }

    private fun sampleProfile() = ProviderProfile(
        id = 12,
        name = "Juan",
        surname = "Gómez",
        profilePhotoUrl = null,
        category = ProviderCategory(1, "Plomería"),
        ratingAverage = 0.0,
        ratingCount = 0,
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
        private val profileProvider: () -> ProviderProfile,
    ) : ProviderProfileRepository {
        override suspend fun getProviderProfile(providerId: Int): ProviderProfileOutcome =
            ProviderProfileOutcome.Success(profileProvider())
    }
}

package com.loresuelvo.consumer.bdd.providerprofile

import com.loresuelvo.consumer.domain.provider.ProviderCategory
import com.loresuelvo.consumer.domain.provider.ProviderCompletionReport
import com.loresuelvo.consumer.domain.provider.ProviderProfile
import com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome
import com.loresuelvo.consumer.domain.provider.ProviderProfileRepository
import com.loresuelvo.consumer.domain.provider.ProviderReview
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

    fun showProviderProfile() {
        profile = sampleProfile()
    }

    fun markProviderAsRated() {
        profile = profile.copy(ratingAverage = 4.5, ratingCount = 2)
    }

    fun markProviderAsCompletedWork() {
        profile = profile.copy(workOrders = completedWorkOrders())
    }

    fun markProviderAsSingleCompletedWork() {
        profile = profile.copy(workOrders = listOf(completedWorkOrders().first()))
    }

    fun markProviderAsReviewedWork() {
        profile = profile.copy(workOrders = listOf(reviewedWorkOrder()))
    }

    fun markProviderAsMultipleCompletedWork() {
        profile = profile.copy(workOrders = completedWorkOrders())
    }

    fun markSomeWorkAsReviewed() {
        profile = profile.copy(
            workOrders = completedWorkOrders().mapIndexed { index, workOrder ->
                if (index == 0) reviewedWorkOrder().copy(id = workOrder.id) else workOrder
            },
        )
    }

    fun markProviderWithoutReviews() {
        profile = profile.copy(ratingAverage = 0.0, ratingCount = 0)
    }

    fun markProviderWithoutCompletedWork() {
        profile = profile.copy(workOrders = emptyList())
    }

    fun markProviderAsUnreviewedWork() {
        profile = profile.copy(
            workOrders = listOf(
                reviewedWorkOrder().copy(review = null),
            ),
        )
    }

    fun markProviderAsPrivateEvidenceWork() {
        profile = profile.copy(workOrders = completedWorkOrders().take(1))
    }

    fun openProviderProfile() {
        scope.runTest {
            viewModel.load(profile.id)
            advanceUntilIdle()
        }
    }

    fun readyProfile(): ProviderProfile =
        (viewModel.uiState.value as ProviderProfileUiState.Ready).profile

    fun publicHistoryRepresentation(): List<String> =
        readyProfile().workOrders.map { workOrder ->
            listOf(
                workOrder.id,
                workOrder.description,
                workOrder.completionReport?.description,
                workOrder.review?.description,
            ).filterNotNull().joinToString(" ")
        }

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
                scheduledOnEpochMillis = 1_755_273_600_000,
                description = "Reparación de pérdida de agua en cocina.",
                status = ProviderWorkOrderStatus.Paid,
                completionReport = ProviderCompletionReport(
                    description = "Trabajo finalizado y funcionamiento verificado.",
                    reportedOnEpochMillis = 1_755_277_200_000,
                ),
                review = ProviderReview(
                    rating = 5,
                    description = "Trabajo prolijo y excelente atención.",
                ),
            ),
        ),
    )

    private fun completedWorkOrders() = listOf(
        reviewedWorkOrder().copy(id = "newest", scheduledOnEpochMillis = 1_755_273_600_000),
        ProviderWorkOrder(
            id = "oldest",
            scheduledOnEpochMillis = 1_755_187_200_000,
            description = "Instalación de artefacto.",
            status = ProviderWorkOrderStatus.Finished,
            completionReport = ProviderCompletionReport(
                description = "Instalación probada y entregada.",
                reportedOnEpochMillis = 1_755_190_800_000,
            ),
            review = null,
        ),
    )

    private fun reviewedWorkOrder() = ProviderWorkOrder(
        id = "reviewed",
        scheduledOnEpochMillis = 1_755_273_600_000,
        description = "Reparación de pérdida de agua en cocina.",
        status = ProviderWorkOrderStatus.Paid,
        completionReport = ProviderCompletionReport(
            description = "Trabajo finalizado y funcionamiento verificado.",
            reportedOnEpochMillis = 1_755_277_200_000,
        ),
        review = ProviderReview(
            rating = 5,
            description = "Trabajo prolijo y excelente atención.",
        ),
    )

    private class FakeRepository(
        private val profileProvider: () -> ProviderProfile,
    ) : ProviderProfileRepository {
        override suspend fun getProviderProfile(providerId: Int): ProviderProfileOutcome =
            ProviderProfileOutcome.Success(profileProvider())
    }
}

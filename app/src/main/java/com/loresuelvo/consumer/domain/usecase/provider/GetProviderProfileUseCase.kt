package com.loresuelvo.consumer.domain.usecase.provider

import com.loresuelvo.consumer.domain.provider.ProviderProfile
import com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome
import com.loresuelvo.consumer.domain.provider.ProviderProfileRepository
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrderStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetProviderProfileUseCase @Inject constructor(
    private val repository: ProviderProfileRepository,
) {
    suspend operator fun invoke(providerId: Int): ProviderProfileOutcome =
        when (val outcome = repository.getProviderProfile(providerId)) {
            is ProviderProfileOutcome.Success ->
                ProviderProfileOutcome.Success(outcome.profile.publicHistory())
            is ProviderProfileOutcome.Failure -> outcome
        }

    private fun ProviderProfile.publicHistory(): ProviderProfile = copy(
        workOrders = workOrders
            .filter { it.status in COMPLETED_STATUSES }
            .sortedByDescending { it.scheduledOnEpochMillis },
    )

    private companion object {
        val COMPLETED_STATUSES = setOf(
            ProviderWorkOrderStatus.AwaitingPayment,
            ProviderWorkOrderStatus.Paid,
            ProviderWorkOrderStatus.Finished,
        )
    }
}

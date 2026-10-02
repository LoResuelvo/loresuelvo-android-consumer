package com.loresuelvo.consumer.bdd.providers.search

import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.provider.ProvidersOutcome
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import java.util.concurrent.atomic.AtomicReference

class FakeProviderRepository(
    seed: List<Provider> = emptyList(),
) : ProviderRepository {

    private val overridesByCategory = mutableMapOf<Int, List<Provider>>()
    private val failureOutcome = AtomicReference<ProvidersOutcome.Failure?>(null)

    private val providers = seed.toMutableList()

    override suspend fun getProvidersByCategory(
        categoryId: Int,
    ): ProvidersOutcome {
        failureOutcome.get()?.let { return it }

        val override = overridesByCategory[categoryId]
        if (override != null) {
            return ProvidersOutcome.Success(override)
        }
        return ProvidersOutcome.Success(providers.filter { it.categoryId == categoryId })
    }

    fun setSeed(providers: List<Provider>) {
        this.providers.clear()
        this.providers.addAll(providers)
    }

    fun setOverrideForCategory(categoryId: Int, providers: List<Provider>) {
        overridesByCategory[categoryId] = providers
    }

    fun setFailure(outcome: ProvidersOutcome.Failure) {
        failureOutcome.set(outcome)
    }

    fun clearFailure() {
        failureOutcome.set(null)
    }
}

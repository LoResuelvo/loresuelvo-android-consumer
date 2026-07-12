package com.loresuelvo.consumer.bdd.providers.search

import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.provider.ProvidersOutcome
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory implementation of [ProviderRepository] used by the BDD
 * scenarios. The seed [providers] come from the Background of the
 * feature file; per-category overrides set by the scenarios
 * (`Given no providers exist for category "Gas"`) take precedence.
 *
 * Mirrors the pattern of `FakeUserRepository` /
 * `FakeAuthSessionStore` in `bdd/onboarding/registerconsumer`.
 */
class FakeProviderRepository(
    seed: List<Provider> = emptyList(),
) : ProviderRepository {

    /**
     * If non-null, the scenario forced a specific outcome for the
     * given [Provider]s. Looked up by `categoryId` per call so that
     * scenarios can pre-load the "no providers for category X"
     * state without us caring about its internal shape.
     */
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

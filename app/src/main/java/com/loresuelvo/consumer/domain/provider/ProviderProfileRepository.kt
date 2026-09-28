package com.loresuelvo.consumer.domain.provider

interface ProviderProfileRepository {
    suspend fun getProviderProfile(providerId: Int): ProviderProfileOutcome
}

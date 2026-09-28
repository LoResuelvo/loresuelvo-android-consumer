package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.api.ApiProviderProfileRepository
import com.loresuelvo.consumer.domain.provider.ProviderProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderProfileRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProviderProfileRepository(
        impl: ApiProviderProfileRepository,
    ): ProviderProfileRepository
}

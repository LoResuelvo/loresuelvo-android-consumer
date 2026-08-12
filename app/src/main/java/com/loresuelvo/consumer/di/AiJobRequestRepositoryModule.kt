package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.api.ApiAiJobRequestRepository
import com.loresuelvo.consumer.domain.jobrequest.AiJobRequestRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds [AiJobRequestRepository] to its production
 * [ApiAiJobRequestRepository] implementation. Lives in its own
 * Hilt module so that acceptance tests which
 * `@UninstallModules(RepositoryModule::class)` to swap the rest
 * of the repo layer (auth, providers, …) still see a valid
 * binding for the AI contact flow when the chat surface is
 * reachable. Tests that want to override THIS specific binding
 * too can `@UninstallModules(AiJobRequestRepositoryModule::class)`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiJobRequestRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAiJobRequestRepository(
        impl: ApiAiJobRequestRepository,
    ): AiJobRequestRepository
}

package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.api.ApiAiConversationRepository
import com.loresuelvo.consumer.domain.assistant.AiConversationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds [AiConversationRepository] to its production
 * [ApiAiConversationRepository] implementation. Lives in its own
 * Hilt module so that acceptance tests which
 * `@UninstallModules(RepositoryModule::class)` to swap the rest
 * of the repo layer (auth, providers, …) still see a valid
 * binding for the AI session list when the bottom-bar
 * "Asistente IA" tab is reachable. Tests that want to override
 * THIS specific binding too can
 * `@UninstallModules(AiConversationRepositoryModule::class)`.
 *
 * Mirrors the same isolation pattern used by
 * `AiJobRequestRepositoryModule` (added for the AI contact flow).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiConversationRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAiConversationRepository(
        impl: ApiAiConversationRepository,
    ): AiConversationRepository
}

package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.api.ApiUserRepository
import com.loresuelvo.consumer.domain.auth.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds domain [UserRepository] ports to their [ApiUserRepository]
 * implementations. The data layer never appears in the public
 * surface of the domain; only this module knows the concrete class.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: ApiUserRepository): UserRepository
}

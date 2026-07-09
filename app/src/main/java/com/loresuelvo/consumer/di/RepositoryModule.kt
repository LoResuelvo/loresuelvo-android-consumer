package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.api.ApiUserRepository
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds every `domain.XxxRepository` port to its production
 * `data/.../XxxRepositoryImpl`. Add a new port by:
 *   1. declaring `interface XxxRepository` in `domain/`;
 *   2. writing `class XxxRepositoryImpl @Inject constructor(...) : XxxRepository` in `data/`;
 *   3. adding a `@Binds fun bindXxxRepository(impl: XxxRepositoryImpl): XxxRepository`
 *      line here.
 *
 * `AuthSessionStore` is registered here because it plays the role of a
 * persistent repository from the consumer-flow perspective. Its
 * `EncryptedSharedPreferences` and the `Auth0Config` provider live in
 * `data/auth/`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: ApiUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindAuthSessionStore(impl: EncryptedAuthSessionStore): AuthSessionStore
}

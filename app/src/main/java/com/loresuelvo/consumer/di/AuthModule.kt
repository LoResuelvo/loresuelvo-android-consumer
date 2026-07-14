package com.loresuelvo.consumer.di

import com.auth0.android.Auth0
import com.loresuelvo.consumer.BuildConfig
import com.loresuelvo.consumer.data.auth.Auth0AuthProvider
import com.loresuelvo.consumer.data.auth.Auth0Config
import com.loresuelvo.consumer.data.auth.Auth0SdkWebAuthLauncher
import com.loresuelvo.consumer.data.auth.Auth0WebAuthLauncher
import com.loresuelvo.consumer.domain.auth.AuthProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt wiring for the Auth0 adapter layer.
 *
 * - `@Binds` the two ports (`AuthProvider`, `Auth0WebAuthLauncher`)
 *   to their production implementations.
 * - `@Provides` [Auth0Config] from `BuildConfig.AUTH0_*` so the SDK
 *   is never instantiated by hand.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthProvider(impl: Auth0AuthProvider): AuthProvider

    @Binds
    @Singleton
    abstract fun bindAuth0WebAuthLauncher(
        impl: Auth0SdkWebAuthLauncher,
    ): Auth0WebAuthLauncher

    companion object {
        @Provides
        @Singleton
        fun provideAuth0Config(): Auth0Config = Auth0Config(
            domain = BuildConfig.AUTH0_DOMAIN,
            clientId = BuildConfig.AUTH0_CLIENT_ID,
            scheme = BuildConfig.AUTH0_SCHEME,
            audience = BuildConfig.AUTH0_AUDIENCE,
        )

        /**
         * Reference to [com.auth0.android.Auth0] for the SDK
         * providers in this module. Kept as a `@Provides` so anyone
         * who wants to raw-test against the SDK can also inject it.
         */
        @Provides
        @Singleton
        fun provideAuth0(config: Auth0Config): Auth0 = Auth0(config.clientId, config.domain)
    }
}

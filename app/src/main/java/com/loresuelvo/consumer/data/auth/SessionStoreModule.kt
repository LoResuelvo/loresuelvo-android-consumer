package com.loresuelvo.consumer.data.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the [SharedPreferences] instance the
 * [EncryptedAuthSessionStore] reads from / writes to. Lives in
 * `data/auth/` so it can call the internal
 * `createEncryptedSessionPrefs(context)` helper (which builds an
 * AES256_GCM/SIV-encrypted blob) without widening that helper's
 * visibility to `di/`.
 *
 * The repository binding to `AuthSessionStore` lives in
 * `di/RepositoryModule.kt`. The previously-bundled
 * `@Provides fun provideAuthSessionStore(...)` factory was removed
 * in Fase 8 of the master plan: `EncryptedAuthSessionStore` is now
 * Hilt-injectable via `@Inject constructor(prefs)`, which removes
 * the last process-wide `object` global mutable (`SessionStateHolder`)
 * from the production graph.
 */
@Module
@InstallIn(SingletonComponent::class)
object SessionStoreModule {

    @Provides
    @Singleton
    fun provideSessionPrefs(
        @ApplicationContext context: Context,
    ): SharedPreferences = createEncryptedSessionPrefs(context)
}

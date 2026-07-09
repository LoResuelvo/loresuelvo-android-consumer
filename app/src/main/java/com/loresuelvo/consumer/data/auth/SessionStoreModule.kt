package com.loresuelvo.consumer.data.auth

import android.content.Context
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds [AuthSessionStore] to its
 * [EncryptedAuthSessionStore] implementation. Lives in `data/auth/`
 * so it can call the internal `createEncryptedSessionPrefs`
 * without widening the visibility of that helper.
 *
 * The `SessionStateHolder` `object` referenced by the implementation
 * remains a process-wide singleton in this phase; Fase 8 of the
 * master plan will migrate it to a Hilt-provided `@Singleton` class
 * so the binding can be fully replaced in tests.
 */
@Module
@InstallIn(SingletonComponent::class)
object SessionStoreModule {

    @Provides
    @Singleton
    fun provideAuthSessionStore(
        @ApplicationContext context: Context,
    ): AuthSessionStore = EncryptedAuthSessionStore(createEncryptedSessionPrefs(context))
}

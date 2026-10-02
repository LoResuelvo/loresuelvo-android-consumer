package com.loresuelvo.consumer.bdd.onboarding.registerconsumer

import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FakeAuthSessionStore(
    initialSession: AuthSession? = null,
) : AuthSessionStore {

    private val state: MutableStateFlow<AuthSession?> = MutableStateFlow(initialSession)

    override val sessionFlow: StateFlow<AuthSession?> get() = state

    override fun getSession(): AuthSession? = state.value

    override fun saveSession(session: AuthSession) {
        state.update { session }
    }

    override fun clearSession() {
        state.update { null }
    }

    /** Replaces the underlying state. Used to seed an authenticated session. */
    fun set(session: AuthSession?) {
        state.update { session }
    }
}

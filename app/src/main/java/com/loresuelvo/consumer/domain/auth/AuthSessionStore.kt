package com.loresuelvo.consumer.domain.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthSessionStore {

    val sessionFlow: StateFlow<AuthSession?>

    fun getSession(): AuthSession?

    /** Persists credentials without publishing them to the navigation flow. */
    fun persistSession(session: AuthSession) {
        saveSession(session)
    }

    fun saveSession(session: AuthSession)

    fun clearSession()
}

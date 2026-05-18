package com.loresuelvo.consumer.domain.auth

interface AuthSessionStore {

    fun getSession(): AuthSession?

    fun saveSession(session: AuthSession)
}

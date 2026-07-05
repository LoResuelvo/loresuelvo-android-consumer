package com.loresuelvo.consumer.domain.auth

interface AuthProvider {

    fun signup()

    var onAuthenticated: (AuthSession) -> Unit

    var onAuthenticationError: (String) -> Unit
}

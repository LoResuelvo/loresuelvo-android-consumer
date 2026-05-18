package com.loresuelvo.consumer.data.auth

import com.auth0.android.result.Credentials
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User

class Auth0CredentialsMapper {

    fun toSession(credentials: Credentials): AuthSession? {
        val profile = credentials.user
        val displayName = firstNonBlank(
            profile.name,
            profile.givenName,
            profile.nickname
        ) ?: DEFAULT_DISPLAY_NAME

        return AuthSession(
            user = User(
                displayName = displayName,
                email = profile.email
            )
        )
    }

    private fun firstNonBlank(
        vararg values: String?
    ): String? = values.firstOrNull { !it.isNullOrBlank() }?.trim()

    private companion object {
        const val DEFAULT_DISPLAY_NAME = "Usuario"
    }
}

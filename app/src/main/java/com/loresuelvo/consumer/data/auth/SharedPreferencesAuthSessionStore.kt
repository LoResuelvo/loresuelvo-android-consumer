package com.loresuelvo.consumer.data.auth

import android.content.Context
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.User

class SharedPreferencesAuthSessionStore(
    context: Context
) : AuthSessionStore {

    private val preferences = context.getSharedPreferences(
        "auth_session",
        Context.MODE_PRIVATE
    )

    override fun getSession(): AuthSession? {
        val displayName = preferences.getString(KEY_DISPLAY_NAME, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return AuthSession(
            user = User(
                displayName = displayName,
                email = preferences.getString(KEY_EMAIL, null)
            )
        )
    }

    override fun saveSession(session: AuthSession) {
        preferences
            .edit()
            .putString(KEY_DISPLAY_NAME, session.user.displayName)
            .putString(KEY_EMAIL, session.user.email)
            .apply()
    }

    override fun clearSession() {
        preferences
            .edit()
            .clear()
            .apply()
    }

    private companion object {
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_EMAIL = "email"
    }
}

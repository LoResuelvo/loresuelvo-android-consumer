package com.loresuelvo.consumer.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedPreferencesAuthSessionStore(
    context: Context
) : AuthSessionStore {

    private val preferences = context.getSharedPreferences(
        "auth_session",
        Context.MODE_PRIVATE
    )

    private val _sessionFlow = MutableStateFlow<AuthSession?>(readSession())
    override val sessionFlow: StateFlow<AuthSession?> = _sessionFlow.asStateFlow()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _sessionFlow.value = readSession()
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun getSession(): AuthSession? = readSession()

    override fun saveSession(session: AuthSession) {

        preferences
            .edit()
            .putString(KEY_DISPLAY_NAME, session.user.displayName)
            .putString(KEY_FIRST_NAME, session.user.firstName)
            .putString(KEY_LAST_NAME, session.user.lastName)
            .putString(KEY_EMAIL, session.user.email)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .commit()
    }

    override fun clearSession() {

        preferences
            .edit()
            .clear()
            .commit()
    }

    private fun readSession(): AuthSession? {

        val displayName = preferences.getString(
            KEY_DISPLAY_NAME,
            null
        )?.takeIf { it.isNotBlank() }
            ?: return null

        val accessToken = preferences.getString(
            KEY_ACCESS_TOKEN,
            null
        ) ?: return null

        return AuthSession(
            user = User(
                displayName = displayName,
                firstName = preferences.getString(KEY_FIRST_NAME, null),
                lastName = preferences.getString(KEY_LAST_NAME, null),
                email = preferences.getString(KEY_EMAIL, null)
            ),
            accessToken = accessToken
        )
    }

    private companion object {
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_FIRST_NAME = "first_name"
        const val KEY_LAST_NAME = "last_name"
        const val KEY_EMAIL = "email"
        const val KEY_ACCESS_TOKEN = "access_token"
    }
}

package com.loresuelvo.consumer.ui.session

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val preferences: SharedPreferences =
        application.getSharedPreferences(KEY_PREFS, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(computeState(readSession()))
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _uiState.value = computeState(readSession())
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onCleared() {
        super.onCleared()
        preferences.unregisterOnSharedPreferenceChangeListener(prefsListener)
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

    private fun computeState(session: AuthSession?): SessionUiState {
        return SessionUiState(
            loading = false,
            authenticated = session != null,
            profileCompleted = session?.user?.isProfileComplete() == true,
        )
    }

    private companion object {
        const val KEY_PREFS = "auth_session"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_FIRST_NAME = "first_name"
        const val KEY_LAST_NAME = "last_name"
        const val KEY_EMAIL = "email"
        const val KEY_ACCESS_TOKEN = "access_token"
    }
}
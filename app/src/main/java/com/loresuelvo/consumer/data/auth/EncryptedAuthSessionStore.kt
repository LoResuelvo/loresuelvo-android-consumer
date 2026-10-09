package com.loresuelvo.consumer.data.auth

import android.content.SharedPreferences
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Encrypted [SharedPreferences]-backed [AuthSessionStore]. Owns the
 * `MutableStateFlow<AuthSession?>` so the in-memory cache and the
 * persisted blob never disagree: every `saveSession` / `clearSession`
 * writes to both, in that order.
 *
 * `@Singleton` so the cached session is shared across the whole
 * process — multiple call sites (the navigation graph, the
 * `SessionViewModel`, the `AuthInterceptor` for the OkHttp client)
 * all observe the SAME flow. The previous process-wide
 * `SessionStateHolder` `object` was migrated here in Fase 8 of the
 * master plan to remove the last `object` global mutable in the
 * production graph.
 */
@Singleton
class EncryptedAuthSessionStore @Inject constructor(
    private val preferences: SharedPreferences,
) : AuthSessionStore {

    private val _sessionFlow: MutableStateFlow<AuthSession?> =
        MutableStateFlow(readSession())

    override val sessionFlow: StateFlow<AuthSession?> = _sessionFlow.asStateFlow()

    init {
        // Defensive: the StateFlow already mirrors `readSession()`. If
        // the SharedPreferences were stale (write from another process
        // or a crash between write and `setValue`), this `init` is a
        // no-op because we read at construction time.
        _sessionFlow.update { it ?: readSession() }
    }

    @Synchronized
    override fun getSession(): AuthSession? = readSession()

    @Synchronized
    override fun persistSession(session: AuthSession) {
        preferences
            .edit()
            .putString(KEY_DISPLAY_NAME, session.user.displayName)
            .putString(KEY_FIRST_NAME, session.user.firstName)
            .putString(KEY_LAST_NAME, session.user.lastName)
            .putString(KEY_EMAIL, session.user.email)
            .putString(KEY_PROFILE_PHOTO_URL, session.user.profilePhotoUrl)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .apply {
                session.user.backendUserId?.let { putInt(KEY_BACKEND_USER_ID, it) }
                    ?: remove(KEY_BACKEND_USER_ID)
            }
            .putString(KEY_ADDRESS_STREET, session.user.address?.street)
            .putString(KEY_ADDRESS_NUMBER, session.user.address?.streetNumber)
            .putString(KEY_ADDRESS_FLOOR, session.user.address?.floor)
            .putString(KEY_ADDRESS_UNIT, session.user.address?.unit)
            .commit()
    }

    @Synchronized
    override fun saveSession(session: AuthSession) {
        persistSession(session)
        _sessionFlow.value = session
    }

    @Synchronized
    override fun clearSession() {
        preferences
            .edit()
            .clear()
            .commit()

        _sessionFlow.value = null
    }

    @Synchronized
    override fun clearSessionIfTokenMatches(accessToken: String): Boolean {
        if (readSession()?.accessToken != accessToken) return false
        clearSession()
        return true
    }

    private fun readSession(): AuthSession? {
        val displayName = preferences
            .getString(KEY_DISPLAY_NAME, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)
            ?: return null

        return AuthSession(
            user = User(
                displayName = displayName,
                firstName = preferences.getString(KEY_FIRST_NAME, null),
                lastName = preferences.getString(KEY_LAST_NAME, null),
                email = preferences.getString(KEY_EMAIL, null),
                profilePhotoUrl = preferences.getString(KEY_PROFILE_PHOTO_URL, null),
                backendUserId = if (preferences.contains(KEY_BACKEND_USER_ID)) preferences.getInt(KEY_BACKEND_USER_ID, 0) else null,
                address = readAddress(),
            ),
            accessToken = accessToken,
        )
    }

    private fun readAddress(): RegisterConsumerAddress? {
        val street = preferences.getString(KEY_ADDRESS_STREET, null) ?: return null
        val streetNumber = preferences.getString(KEY_ADDRESS_NUMBER, null) ?: return null
        return RegisterConsumerAddress(
            street = street,
            streetNumber = streetNumber,
            floor = preferences.getString(KEY_ADDRESS_FLOOR, "").orEmpty(),
            unit = preferences.getString(KEY_ADDRESS_UNIT, "").orEmpty(),
        )
    }

    private companion object {
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_FIRST_NAME = "first_name"
        const val KEY_LAST_NAME = "last_name"
        const val KEY_EMAIL = "email"
        const val KEY_PROFILE_PHOTO_URL = "profile_photo_url"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_BACKEND_USER_ID = "backend_user_id"
        const val KEY_ADDRESS_STREET = "address_street"
        const val KEY_ADDRESS_NUMBER = "address_number"
        const val KEY_ADDRESS_FLOOR = "address_floor"
        const val KEY_ADDRESS_UNIT = "address_unit"
    }
}

package com.loresuelvo.consumer.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.BuildConfig
import com.loresuelvo.consumer.domain.auth.AuthProvider
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.LogoutOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel that mirrors the [AuthSessionStore]'s flow into a
 * Compose-friendly [SessionUiState]. Replaces the old
 * `SessionStateHolder`-based implementation (Fase 8 of the master
 * plan): the VM is now injected via Hilt and observes the
 * `sessionFlow` exposed by the production-ready
 * `EncryptedAuthSessionStore`.
 *
 * `SessionUiState.loading` is always `false` once the first
 * `sessionFlow.value` has been read at construction — auth is a
 * synchronous read, not an asynchronous one. `WelcomeViewModel`
 * owns its own loading flag for the IdP signup itself.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionStore: AuthSessionStore,
    private val authProvider: AuthProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(computeState(sessionStore.sessionFlow.value))
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.sessionFlow.collect { session ->
                _uiState.update { current -> computeState(session) }
            }
        }
    }

    /**
     * **Local-first sign-out.** Clears the cached session synchronously
     * so the smart-router in `LoResuelvoNav` observes the change and
     * pops back to `Welcome` before this function returns to the click
     * handler. The Auth0 SSO logout is dispatched in the background:
     * its result (Success / Cancelled / Failure) does not gate the
     * local sign-out — by the time the user could react to an Auth0
     * failure they are already at the login screen.
     *
     * The previous policy ("Auth0 first, then local") caused the
     * consumer to stay on the Home screen when the Auth0 browser
     * stayed open (browser not closed → callback not invoked →
     * session not cleared → smart-router never re-routes). The new
     * policy eliminates that edge case at the cost of leaving the
     * Auth0 SDK token alive until the next successful login. The
     * SDK token alone does not grant local access because the
     * smart-router checks the local `EncryptedSharedPreferences`
     * session, not the SDK state.
     */
    fun signOut(activityContext: Context) {
        // 1. Clear the local session BEFORE returning so the
        // smart-router re-routes to Welcome synchronously.
        sessionStore.clearSession()
        // 2. Dispatch the Auth0 SSO logout in the background.
        // Its result is fire-and-forget; we log it for diagnostics
        // but the consumer has already left Home for Welcome.
        viewModelScope.launch {
            val outcome = authProvider.logout(activityContext)
            if (BuildConfig.DEBUG && outcome is LogoutOutcome.Failure) {
                android.util.Log.w(
                    "SessionViewModel",
                    "Auth0 logout failed after local sign-out; " +
                        "user will re-authenticate fresh next login",
                )
            }
        }
    }

    private fun computeState(session: AuthSession?): SessionUiState =
        SessionUiState(loading = false, session = session)
}

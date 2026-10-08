package com.loresuelvo.consumer.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.BuildConfig
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.LogoutOutcome
import com.loresuelvo.consumer.domain.usecase.auth.RestoreAuthenticatedSessionUseCase
import com.loresuelvo.consumer.platform.auth.AuthProvider
import com.loresuelvo.consumer.domain.installation.InstallationRegistrationRequests
import com.loresuelvo.consumer.domain.notifications.NotificationDismissal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionStore: AuthSessionStore,
    private val authProvider: AuthProvider,
    private val restoreSession: RestoreAuthenticatedSessionUseCase,
    private val pushRegistrationRequests: InstallationRegistrationRequests,
    private val notificationDismissal: NotificationDismissal = NotificationDismissal { },
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SessionUiState(
            loading = sessionStore.sessionFlow.value != null,
            session = sessionStore.sessionFlow.value,
        ),
    )
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()
    private var restorationJob: Job? = null

    init {
        retryRestoration()
        viewModelScope.launch {
            sessionStore.sessionFlow.collect { session ->
                val currentState = _uiState.value
                val canUpdateSession = restorationJob?.isActive != true &&
                    (currentState.error == null || currentState.session != session)
                if (session == null || canUpdateSession) {
                    _uiState.value = computeState(session)
                }
            }
        }
    }

    fun retryRestoration() {
        if (restorationJob?.isActive == true) return
        val session = sessionStore.sessionFlow.value ?: return
        _uiState.value = SessionUiState(loading = true, session = session)
        restorationJob = viewModelScope.launch {
            val outcome = restoreSession(session)
            val current = sessionStore.sessionFlow.value
            val failure = outcome is CurrentUserOutcome.Failure.Network ||
                outcome is CurrentUserOutcome.Failure.Server
            if (!failure && current?.user?.backendUserId != null && current.user.backendUserId > 0) {
                pushRegistrationRequests.request()
            }
            _uiState.value = SessionUiState(
                loading = false,
                session = current,
                error = if (failure && current == session) SessionError.Restoration else null,
            )
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
        pushRegistrationRequests.invalidate()
        // 1. Clear the local session BEFORE returning so the
        // smart-router re-routes to Welcome synchronously.
        restorationJob?.cancel()
        sessionStore.clearSession()
        notificationDismissal.dismissAll()
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

package com.loresuelvo.consumer.ui.session

import com.loresuelvo.consumer.domain.auth.AuthSession

/**
 * UDF state for the `Session`-derived screens (the smart router in
 * [com.loresuelvo.consumer.ui.navigation.LoResuelvoNav] and the
 * `Home` screen).
 *
 * Carrying the full [AuthSession] keeps consumers framework-free —
 * `HomeScreen` reads `firstName / lastName` for display, and the
 * token never leaves this object. The `authenticated` and
 * `profileCompleted` derived flags are exposed so the navigation
 * graph can route without recomputing them at every call site.
 */
data class SessionUiState(
    val loading: Boolean = true,
    val session: AuthSession? = null,
    val signingOut: Boolean = false,
    val error: SessionError? = null,
) {
    val authenticated: Boolean get() = session != null
    val profileCompleted: Boolean get() = session?.user?.isProfileComplete() == true
}

sealed interface SessionError {
    data object Logout : SessionError
}

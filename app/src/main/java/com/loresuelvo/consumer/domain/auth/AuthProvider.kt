package com.loresuelvo.consumer.domain.auth

import android.content.Context

/**
 * Port that abstracts the identity-provider layer (Auth0 today, any
 * OIDC provider tomorrow) behind a single `signup` method.
 *
 * `signup(context)` requires an Activity-bound [Context] because the
 * underlying SDK launches an in-app WebView flow via `startActivity`
 * — passing the application context silently fails. The Activity
 * context is supplied by the Composable call site via
 * `LocalContext.current`, not by the ViewModel — keeping the VM
 * `@HiltViewModel`-clean.
 */
interface AuthProvider {

    /**
     * Opens the IdP signup flow on [context]. Returns a typed
     * [SignupOutcome] when the user dismisses the flow, succeeds, or
     * the network fails. Never throws.
     */
    suspend fun signup(context: Context): SignupOutcome
}

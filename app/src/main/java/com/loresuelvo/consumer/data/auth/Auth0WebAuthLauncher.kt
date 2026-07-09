package com.loresuelvo.consumer.data.auth

import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the SDK-level knobs (Auth0 account + scheme) used by
 * `Auth0SdkWebAuthLauncher`. Built from `BuildConfig` values inside
 * `AuthModule.provideAuth0Config`; kept here so the launcher stays
 * SDK-only.
 */
data class Auth0Config(
    val domain: String,
    val clientId: String,
    val scheme: String,
)

/**
 * Port for launching the Auth0 signup WebView. A fake implementation
 * lives in `Auth0AuthProviderTest`; the production implementation
 * [Auth0SdkWebAuthLauncher] is the only consumer in app code.
 */
interface Auth0WebAuthLauncher {
    fun startSignup(
        context: Context,
        callback: Callback<Credentials, AuthenticationException>,
    )
}

/**
 * Auth0-SDK-backed [Auth0WebAuthLauncher]. `@Inject` so Hilt wires
 * the [Auth0Config] from `BuildConfig` automatically. The launcher
 * itself is `@Singleton` because the Auth0 account and scheme are
 * effectively immutable for the process lifetime.
 */
@Singleton
class Auth0SdkWebAuthLauncher @Inject constructor(
    private val config: Auth0Config,
) : Auth0WebAuthLauncher {

    override fun startSignup(
        context: Context,
        callback: Callback<Credentials, AuthenticationException>,
    ) {
        val account = Auth0(config.clientId, config.domain)
        WebAuthProvider
            .login(account)
            .withScheme(config.scheme)
            .withScreenHint("signup")
            .start(context, callback)
    }
}

private fun WebAuthProvider.Builder.withScreenHint(
    screenHint: String,
): WebAuthProvider.Builder = withParameters(mapOf("screen_hint" to screenHint))

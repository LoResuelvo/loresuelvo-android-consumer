package com.loresuelvo.consumer.data.auth

import android.content.Context
import android.util.Log
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials

interface Auth0WebAuthLauncher {

    fun startSignup(context: Context)
}

class Auth0SdkWebAuthLauncher(
    private val account: Auth0,
    private val scheme: String
) : Auth0WebAuthLauncher {

    override fun startSignup(context: Context) {
        WebAuthProvider
            .login(account)
            .withScheme(scheme)
            .withScreenHint("signup")
            .start(context, LoggingAuthCallback)
    }
}

private object LoggingAuthCallback : Callback<Credentials, AuthenticationException> {

    override fun onSuccess(result: Credentials) {
        Log.d("Auth0AuthProvider", "Auth0 authentication succeeded")
    }

    override fun onFailure(error: AuthenticationException) {
        Log.w("Auth0AuthProvider", "Auth0 authentication failed", error)
    }
}

private fun WebAuthProvider.Builder.withScreenHint(
    screenHint: String
): WebAuthProvider.Builder = withParameters(mapOf("screen_hint" to screenHint))

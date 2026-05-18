package com.loresuelvo.consumer.data.auth

import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials

interface Auth0WebAuthLauncher {

    fun startSignup(
        context: Context,
        callback: Callback<Credentials, AuthenticationException>
    )
}

class Auth0SdkWebAuthLauncher(
    private val account: Auth0,
    private val scheme: String
) : Auth0WebAuthLauncher {

    override fun startSignup(
        context: Context,
        callback: Callback<Credentials, AuthenticationException>
    ) {
        WebAuthProvider
            .login(account)
            .withScheme(scheme)
            .withScreenHint("signup")
            .start(context, callback)
    }
}

private fun WebAuthProvider.Builder.withScreenHint(
    screenHint: String
): WebAuthProvider.Builder = withParameters(mapOf("screen_hint" to screenHint))

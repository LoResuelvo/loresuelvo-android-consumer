package com.loresuelvo.consumer.data.auth

import android.content.Context
import android.util.Log
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.result.Credentials
import com.loresuelvo.consumer.BuildConfig
import com.loresuelvo.consumer.domain.auth.AuthenticatedUser
import com.loresuelvo.consumer.domain.auth.AuthProvider

class Auth0AuthProvider(
    private val context: Context,
    private val onAuthenticated: (AuthenticatedUser) -> Unit = {},
    private val webAuthLauncher: Auth0WebAuthLauncher = Auth0SdkWebAuthLauncher(
        account = Auth0(
            BuildConfig.AUTH0_CLIENT_ID,
            BuildConfig.AUTH0_DOMAIN
        ),
        scheme = BuildConfig.AUTH0_SCHEME
    )
) : AuthProvider {

    override fun signup() {
        webAuthLauncher.startSignup(context, Auth0SignupCallback(onAuthenticated))
    }
}

private class Auth0SignupCallback(
    private val onAuthenticated: (AuthenticatedUser) -> Unit
) : Callback<Credentials, AuthenticationException> {

    override fun onSuccess(result: Credentials) {
        val profile = result.user
        val name = profile.name
            ?: profile.nickname
            ?: profile.email

        if (name != null) {
            onAuthenticated(AuthenticatedUser(name = name))
        }

        Log.d("Auth0AuthProvider", "Auth0 authentication succeeded")
    }

    override fun onFailure(error: AuthenticationException) {
        Log.w("Auth0AuthProvider", "Auth0 authentication failed", error)
    }
}

package com.loresuelvo.consumer.data.auth

import android.content.Context
import android.util.Log
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.result.Credentials
import com.loresuelvo.consumer.BuildConfig
import com.loresuelvo.consumer.domain.auth.AuthProvider
import com.loresuelvo.consumer.domain.auth.AuthSession

class Auth0AuthProvider(
    private val context: Context,
    override var onAuthenticated: (AuthSession) -> Unit = {},
    override var onAuthenticationError: (String) -> Unit = {},
    private val credentialsMapper: Auth0CredentialsMapper = Auth0CredentialsMapper(),
    private val webAuthLauncher: Auth0WebAuthLauncher = Auth0SdkWebAuthLauncher(
        account = Auth0(
            BuildConfig.AUTH0_CLIENT_ID,
            BuildConfig.AUTH0_DOMAIN
        ),
        scheme = BuildConfig.AUTH0_SCHEME
    )
) : AuthProvider {

    override fun signup() {
        webAuthLauncher.startSignup(
            context,
            Auth0SignupCallback(
                provider = this,
                credentialsMapper = credentialsMapper
            )
        )
    }
}

private class Auth0SignupCallback(
    private val provider: Auth0AuthProvider,
    private val credentialsMapper: Auth0CredentialsMapper
) : Callback<Credentials, AuthenticationException> {

    override fun onSuccess(result: Credentials) {
        credentialsMapper.toSession(result)?.let { session ->
            provider.onAuthenticated(session)
        }

        Log.d("Auth0AuthProvider", "Auth0 authentication succeeded")
    }

    override fun onFailure(error: AuthenticationException) {
        Log.w("Auth0AuthProvider", "Auth0 authentication failed", error)

        if (error.getCode() == "a0.authentication_canceled") {
            return
        }

        provider.onAuthenticationError(
            "No pudimos completar el registro"
        )
    }
}

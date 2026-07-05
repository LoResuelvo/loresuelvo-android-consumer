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
import com.loresuelvo.consumer.domain.auth.SignupOutcome
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class Auth0AuthProvider(
    private val context: Context,
    private val credentialsMapper: Auth0CredentialsMapper = Auth0CredentialsMapper(),
    private val webAuthLauncher: Auth0WebAuthLauncher = Auth0SdkWebAuthLauncher(
        account = Auth0(
            BuildConfig.AUTH0_CLIENT_ID,
            BuildConfig.AUTH0_DOMAIN
        ),
        scheme = BuildConfig.AUTH0_SCHEME
    )
) : AuthProvider {

    override suspend fun signup(): SignupOutcome = suspendCancellableCoroutine { cont ->
        webAuthLauncher.startSignup(
            context,
            Auth0SignupCallback(
                credentialsMapper = credentialsMapper,
                cont = cont
            )
        )
    }
}

private class Auth0SignupCallback(
    private val credentialsMapper: Auth0CredentialsMapper,
    private val cont: kotlinx.coroutines.CancellableContinuation<SignupOutcome>,
) : Callback<Credentials, AuthenticationException> {

    override fun onSuccess(result: Credentials) {
        val session = credentialsMapper.toSession(result)
        if (session != null) {
            cont.resume(SignupOutcome.Success(session))
        } else {
            cont.resume(SignupOutcome.Failed("No pudimos completar el registro"))
        }

        Log.d("Auth0AuthProvider", "Auth0 authentication succeeded")
    }

    override fun onFailure(error: AuthenticationException) {
        Log.w("Auth0AuthProvider", "Auth0 authentication failed", error)

        if (error.getCode() == "a0.authentication_canceled") {
            cont.resume(SignupOutcome.Cancelled)
        } else {
            cont.resume(SignupOutcome.Failed("No pudimos completar el registro"))
        }
    }
}
package com.loresuelvo.consumer.data.auth

import android.content.Context
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.result.Credentials
import com.loresuelvo.consumer.domain.auth.AuthProvider
import com.loresuelvo.consumer.domain.auth.SignupOutcome
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Auth0-backed [AuthProvider]. The Activity [Context] is supplied by
 * the call site (the [com.loresuelvo.consumer.ui.auth.WelcomeViewModel]
 * receives it from `LocalContext.current`); the SDK config
 * ([Auth0SdkWebAuthLauncher], [Auth0CredentialsMapper]) comes through
 * the Hilt graph. No `Context` is captured at construction time, so the
 * provider is `@Singleton`-safe.
 */
@Singleton
class Auth0AuthProvider @Inject constructor(
    private val credentialsMapper: Auth0CredentialsMapper,
    private val webAuthLauncher: Auth0WebAuthLauncher,
) : AuthProvider {

    override suspend fun signup(context: Context): SignupOutcome =
        suspendCancellableCoroutine { cont ->
            webAuthLauncher.startSignup(
                context = context,
                callback = Auth0SignupCallback(credentialsMapper, cont),
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
    }

    override fun onFailure(error: AuthenticationException) {
        if (error.getCode() == "a0.authentication_canceled") {
            cont.resume(SignupOutcome.Cancelled)
        } else {
            cont.resume(SignupOutcome.Failed("No pudimos completar el registro"))
        }
    }
}

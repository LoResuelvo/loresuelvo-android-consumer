package com.loresuelvo.consumer.integration.auth

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.result.Credentials
import com.loresuelvo.consumer.data.auth.Auth0AuthProvider
import com.loresuelvo.consumer.data.auth.Auth0WebAuthLauncher
import com.loresuelvo.consumer.domain.auth.SignupOutcome
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class Auth0AuthProviderTest {

    @Test
    fun signup_opens_auth0_signup() = runBlocking {
        val launcher = FakeAuth0WebAuthLauncher()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authProvider = Auth0AuthProvider(
            context = context,
            webAuthLauncher = launcher
        )

        val result = async { authProvider.signup() }

        while (!launcher.signupStarted) yield()

        launcher.succeedWith(credentialsWithName("Andres"))

        result.await()

        assertTrue(launcher.signupStarted)
    }

    @Test
    fun signup_success_notifies_authenticated_user() = runBlocking {
        val launcher = FakeAuth0WebAuthLauncher()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authProvider = Auth0AuthProvider(
            context = context,
            webAuthLauncher = launcher
        )

        val result = async { authProvider.signup() }

        while (!launcher.signupStarted) yield()

        launcher.succeedWith(credentialsWithName("Andres"))

        val outcome = result.await()

        assertTrue(outcome is SignupOutcome.Success)
        assertEquals("Andres", (outcome as SignupOutcome.Success).session.user.displayName)
    }

    @Test
    fun signup_failure_notifies_authentication_error() = runBlocking {
        val launcher = FakeAuth0WebAuthLauncher()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authProvider = Auth0AuthProvider(
            context = context,
            webAuthLauncher = launcher
        )

        val result = async { authProvider.signup() }

        while (!launcher.signupStarted) yield()

        launcher.failWith(
            AuthenticationException("Auth0 unavailable")
        )

        val outcome = result.await()

        assertTrue(outcome is SignupOutcome.Failed)
        assertEquals(
            "No pudimos completar el registro",
            (outcome as SignupOutcome.Failed).message
        )
    }

    private class FakeAuth0WebAuthLauncher : Auth0WebAuthLauncher {

        var signupStarted = false
            private set
        private var callback: Callback<Credentials, AuthenticationException>? = null

        override fun startSignup(
            context: Context,
            callback: Callback<Credentials, AuthenticationException>
        ) {
            signupStarted = true
            this.callback = callback
        }

        fun succeedWith(credentials: Credentials) {
            callback?.onSuccess(credentials)
        }

        fun failWith(error: AuthenticationException) {
            callback?.onFailure(error)
        }
    }

    private fun credentialsWithName(name: String): Credentials = Credentials(
        idToken = idTokenWithName(name),
        accessToken = "access-token",
        type = "Bearer",
        refreshToken = null,
        expiresAt = Date(System.currentTimeMillis() + 60_000),
        scope = "openid profile email"
    )

    private fun idTokenWithName(name: String): String {
        val header = encodeJwtPart("""{"alg":"none"}""")
        val payload = encodeJwtPart("""{"sub":"auth0|123","name":"$name"}""")
        return "$header.$payload."
    }

    private fun encodeJwtPart(value: String): String =
        Base64.encodeToString(
            value.toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
}
package com.loresuelvo.consumer.integration.auth

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.result.Credentials
import com.loresuelvo.consumer.data.auth.Auth0AuthProvider
import com.loresuelvo.consumer.data.auth.Auth0WebAuthLauncher
import com.loresuelvo.consumer.domain.auth.AuthSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class Auth0AuthProviderTest {

    @Test
    fun signup_opens_auth0_signup() {

        val launcher = FakeAuth0WebAuthLauncher()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authProvider = Auth0AuthProvider(
            context = context,
            webAuthLauncher = launcher
        )

        authProvider.signup()

        assertTrue(launcher.signupStarted)
    }

    @Test
    fun signup_success_notifies_authenticated_user() {

        var authSession: AuthSession? = null
        val launcher = FakeAuth0WebAuthLauncher()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authProvider = Auth0AuthProvider(
            context = context,
            onAuthenticated = { session ->
                authSession = session
            },
            webAuthLauncher = launcher
        )

        authProvider.signup()
        launcher.succeedWith(credentialsWithName("Andres"))

        assertEquals("Andres", authSession?.user?.displayName)
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

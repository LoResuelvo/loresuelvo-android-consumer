package com.loresuelvo.consumer.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.result.Credentials
import com.loresuelvo.consumer.domain.auth.AuthenticationOutcome
import com.loresuelvo.consumer.domain.auth.LogoutOutcome
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

/**
 * Unit tests for [Auth0AuthProvider]. Migrated from the
 * `androidTest/integration/auth/` source set in Fase 8:
 * `Auth0AuthProvider` no longer captures `Context` in its
 * constructor, so the test supplies it per `signup(context)` call,
 * matching the contract enforced by `AuthProvider` (the domain port).
 *
 * `[FakeAuth0WebAuthLauncher]` implements the production
 * `Auth0WebAuthLauncher` interface so the test exercises the real
 * `Auth0AuthProvider.signup(context)` `suspendCancellableCoroutine`
 * branch without any Android SDK call beyond the Context type.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Auth0AuthProviderTest {

    private val context: Context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun signup_opens_auth0_signup() = runBlocking {
        val launcher = FakeAuth0WebAuthLauncher()
        val authProvider = Auth0AuthProvider(
            credentialsMapper = Auth0CredentialsMapper(),
            webAuthLauncher = launcher,
        )

        val result = async { authProvider.signup(context) }

        while (!launcher.signupStarted) yield()

        launcher.succeedWith(credentialsWithName("Andres"))

        result.await()

        assertTrue(launcher.signupStarted)
    }

    @Test
    fun signup_success_notifies_authenticated_user() = runBlocking {
        val launcher = FakeAuth0WebAuthLauncher()
        val authProvider = Auth0AuthProvider(
            credentialsMapper = Auth0CredentialsMapper(),
            webAuthLauncher = launcher,
        )

        val result = async { authProvider.signup(context) }

        while (!launcher.signupStarted) yield()

        launcher.succeedWith(credentialsWithName("Andres"))

        val outcome = result.await()

        assertTrue("expected AuthenticationOutcome.Success, got $outcome", outcome is AuthenticationOutcome.Success)
        val session = (outcome as AuthenticationOutcome.Success).session
        assertEquals("Andres", session.user.displayName)
        assertEquals("fake-access", session.accessToken)
    }

    @Test
    fun signup_failure_notifies_authentication_error() = runBlocking {
        val launcher = FakeAuth0WebAuthLauncher()
        val authProvider = Auth0AuthProvider(
            credentialsMapper = Auth0CredentialsMapper(),
            webAuthLauncher = launcher,
        )

        val result = async { authProvider.signup(context) }

        while (!launcher.signupStarted) yield()

        launcher.failWith(AuthenticationException("Auth0 unavailable"))

        val outcome = result.await()

        assertTrue(
            "expected AuthenticationOutcome.Failure.Provider, got $outcome",
            outcome is AuthenticationOutcome.Failure.Provider,
        )
        assertEquals(
            "Auth0 unavailable",
            (outcome as AuthenticationOutcome.Failure.Provider).cause?.message,
        )
    }

    @Test
    fun logout_success_notifies_completed_logout() = runBlocking {
        val launcher = FakeAuth0WebAuthLauncher()
        val authProvider = Auth0AuthProvider(
            credentialsMapper = Auth0CredentialsMapper(),
            webAuthLauncher = launcher,
        )

        val result = async { authProvider.logout(context) }
        while (!launcher.logoutStarted) yield()
        launcher.succeedLogout()

        assertEquals(LogoutOutcome.Success, result.await())
    }

    // The `AuthenticationOutcome.Cancelled` branch is reached only when
    // `AuthenticationException.getCode() == "a0.authentication_canceled"`,
    // which Auth0 SDK sets internally on user-cancel. The SDK does
    // not expose a constructor that lets us set that code, so the
    // cancel branch is left to instrumented coverage (Phase 9 / CI).

    private class FakeAuth0WebAuthLauncher : Auth0WebAuthLauncher {

        var signupStarted = false
            private set
        var logoutStarted = false
            private set
        private var callback: Callback<Credentials, AuthenticationException>? = null
        private var logoutCallback: Callback<Void?, AuthenticationException>? = null

        override fun startLogin(
            context: Context,
            callback: Callback<Credentials, AuthenticationException>,
        ) {
            signupStarted = true
            this.callback = callback
        }

        override fun startSignup(
            context: Context,
            callback: Callback<Credentials, AuthenticationException>,
        ) {
            signupStarted = true
            this.callback = callback
        }

        override fun startGoogleLogin(
            context: Context,
            callback: Callback<Credentials, AuthenticationException>,
        ) {
            signupStarted = true
            this.callback = callback
        }

        override fun startLogout(
            context: Context,
            callback: Callback<Void?, AuthenticationException>,
        ) {
            logoutStarted = true
            logoutCallback = callback
        }

        fun succeedWith(credentials: Credentials) {
            callback?.onSuccess(credentials)
        }

        fun failWith(error: AuthenticationException) {
            callback?.onFailure(error)
        }

        fun succeedLogout() {
            logoutCallback?.onSuccess(null)
        }
    }

    private fun credentialsWithName(name: String): Credentials = Credentials(
        idToken = idTokenWithName(name),
        accessToken = "fake-access",
        type = "Bearer",
        refreshToken = null,
        expiresAt = Date(System.currentTimeMillis() + 60_000),
        scope = "openid profile email"
    )

    private fun idTokenWithName(name: String): String {
        val header = android.util.Base64.encodeToString(
            """{"alg":"none"}""".toByteArray(),
            android.util.Base64.URL_SAFE
                or android.util.Base64.NO_WRAP
                or android.util.Base64.NO_PADDING,
        )
        val payload = android.util.Base64.encodeToString(
            """{"sub":"auth0|123","name":"$name","email":"$name@pro.com","given_name":"$name"}""".toByteArray(),
            android.util.Base64.URL_SAFE
                or android.util.Base64.NO_WRAP
                or android.util.Base64.NO_PADDING,
        )
        return "$header.$payload."
    }
}

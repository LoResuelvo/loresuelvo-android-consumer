package com.loresuelvo.consumer.integration.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.data.auth.Auth0AuthProvider
import com.loresuelvo.consumer.data.auth.Auth0WebAuthLauncher
import org.junit.Assert.assertTrue
import org.junit.Test

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

    private class FakeAuth0WebAuthLauncher : Auth0WebAuthLauncher {

        var signupStarted = false
            private set

        override fun startSignup(context: Context) {
            signupStarted = true
        }
    }
}

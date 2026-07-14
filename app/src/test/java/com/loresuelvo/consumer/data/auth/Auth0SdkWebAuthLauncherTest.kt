package com.loresuelvo.consumer.data.auth

import com.auth0.android.provider.WebAuthProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Test

class Auth0SdkWebAuthLauncherTest {

    private val config = Auth0Config(
        domain = "tenant.us.auth0.com",
        clientId = "native-client-id",
        scheme = "com.loresuelvo.consumer",
        audience = "http://localhost:8080",
    )

    @Test
    fun should_request_access_token_for_configured_api_audience() {
        val builder = mockk<WebAuthProvider.Builder>()
        every { builder.withScheme(any()) } returns builder
        every { builder.withAudience(any()) } returns builder
        every { builder.withParameters(any()) } returns builder

        builder.configureSignup(config)

        verifyOrder {
            builder.withScheme("com.loresuelvo.consumer")
            builder.withAudience("http://localhost:8080")
            builder.withParameters(mapOf("screen_hint" to "signup"))
        }
    }

    @Test
    fun should_open_login_without_forcing_signup_screen() {
        val builder = mockk<WebAuthProvider.Builder>()
        every { builder.withScheme(any()) } returns builder
        every { builder.withAudience(any()) } returns builder

        builder.configureLogin(config)

        verifyOrder {
            builder.withScheme("com.loresuelvo.consumer")
            builder.withAudience("http://localhost:8080")
        }
    }

    @Test
    fun should_select_google_connection_for_google_login() {
        val builder = mockk<WebAuthProvider.Builder>()
        every { builder.withScheme(any()) } returns builder
        every { builder.withAudience(any()) } returns builder
        every { builder.withConnection(any()) } returns builder

        builder.configureGoogleLogin(config)

        verifyOrder {
            builder.withScheme("com.loresuelvo.consumer")
            builder.withAudience("http://localhost:8080")
            builder.withConnection("google-oauth2")
        }
    }

    @Test
    fun should_return_to_app_after_auth0_logout() {
        val builder = mockk<WebAuthProvider.LogoutBuilder>()
        every { builder.withScheme(any()) } returns builder

        builder.configureLogout(config)

        verifyOrder {
            builder.withScheme("com.loresuelvo.consumer")
        }
    }
}

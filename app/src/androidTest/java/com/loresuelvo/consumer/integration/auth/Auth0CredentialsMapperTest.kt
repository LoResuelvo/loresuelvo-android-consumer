package com.loresuelvo.consumer.data.auth

import android.util.Base64
import com.auth0.android.result.Credentials
import com.loresuelvo.consumer.domain.auth.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Date

class Auth0CredentialsMapperTest {

    private val mapper = Auth0CredentialsMapper()

    @Test
    fun maps_first_name_last_name_and_email_from_auth0_profile() {

        val session = mapper.toSession(
            credentialsWithProfile(
                givenName = "Andres",
                familyName = "Colina",
                email = "andy@pro.com"
            )
        )

        requireNotNull(session)

        assertEquals("Andres", session.user.firstName)
        assertEquals("Colina", session.user.lastName)
        assertEquals("andy@pro.com", session.user.email)

        assertTrue(session.user.isProfileComplete())
    }

    @Test
    fun profile_is_incomplete_when_last_name_is_missing() {

        val user = User(
            displayName = "Andres",
            firstName = "Andres",
            lastName = null
        )

        assertFalse(user.isProfileComplete())
    }
    private fun credentialsWithProfile(
        givenName: String,
        familyName: String,
        email: String
    ): Credentials {

        return Credentials(
            idToken = idToken(
                givenName,
                familyName,
                email
            ),
            accessToken = "access-token",
            type = "Bearer",
            refreshToken = null,
            expiresAt = Date(System.currentTimeMillis() + 60_000),
            scope = "openid profile email"
        )
    }

    private fun idToken(
        givenName: String,
        familyName: String,
        email: String
    ): String {

        val header = encode("""{"alg":"none"}""")

        val payload = encode(
            """
            {
              "sub":"auth0|123",
              "given_name":"$givenName",
              "family_name":"$familyName",
              "email":"$email"
            }
            """.trimIndent()
        )

        return "$header.$payload."
    }

    private fun encode(value: String): String =
        Base64.encodeToString(
            value.toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
}
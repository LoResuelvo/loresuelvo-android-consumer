package com.loresuelvo.consumer.data.auth

import android.util.Base64
import com.auth0.android.result.Credentials
import com.loresuelvo.consumer.domain.auth.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

/**
 * Unit tests for [Auth0CredentialsMapper]. Migrated from the
 * `androidTest/integration/auth/` source set in Fase 8: the SUT now
 * uses `android.util.Base64` and `org.json.JSONObject` to decode the
 * JWT payload, so the test runs under Robolectric instead of an
 * instrumented JUnit runner.
 *
 * The JWT is hand-crafted so the test is independent of any real
 * Auth0 tenant.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
        // Documents the contract: `isProfileComplete()` is false when
        // any of the Auth0 identity fields is null. This is what drives
        // the navigation decision in `LoResuelvoNav`.
        val user = User(
            displayName = "Andres",
            firstName = "Andres",
            lastName = null,
        )

        assertFalse(user.isProfileComplete())
    }

    @Test
    fun returns_null_when_jwt_is_malformed() {
        val credentials = Credentials(
            idToken = "this.is.not.a.valid.jwt",
            accessToken = "access",
            type = "Bearer",
            refreshToken = null,
            expiresAt = Date(System.currentTimeMillis() + 60_000),
            scope = "openid profile email"
        )

        assertNull(mapper.toSession(credentials))
    }

    private fun credentialsWithProfile(
        givenName: String,
        familyName: String,
        email: String,
    ): Credentials = Credentials(
        idToken = idToken(givenName, familyName, email),
        accessToken = "access-token",
        type = "Bearer",
        refreshToken = null,
        expiresAt = Date(System.currentTimeMillis() + 60_000),
        scope = "openid profile email"
    )

    private fun idToken(
        givenName: String,
        familyName: String,
        email: String,
    ): String {
        val header = encodeJwtPart("""{"alg":"none"}""")
        val payload = encodeJwtPart(
            """
            {
              "sub":"auth0|123",
              "given_name":"$givenName",
              "family_name":"$familyName",
              "email":"$email"
            }
            """.trimIndent(),
        )
        return "$header.$payload."
    }

    private fun encodeJwtPart(value: String): String =
        Base64.encodeToString(
            value.toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
}

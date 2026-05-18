package com.loresuelvo.consumer.integration.auth

import android.util.Base64
import com.auth0.android.result.Credentials
import com.loresuelvo.consumer.data.auth.Auth0CredentialsMapper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class Auth0CredentialsMapperTest {

    @Test
    fun maps_profile_name_as_display_name() {

        val session = Auth0CredentialsMapper().toSession(
            credentialsWithClaims(
                """"name":"Andres Colina","email":"andres@example.com""""
            )
        )

        assertEquals("Andres Colina", session?.user?.displayName)
        assertEquals("andres@example.com", session?.user?.email)
    }

    @Test
    fun uses_given_name_before_email_when_name_is_missing() {

        val session = Auth0CredentialsMapper().toSession(
            credentialsWithClaims(
                """"given_name":"Andres","email":"andres@example.com""""
            )
        )

        assertEquals("Andres", session?.user?.displayName)
    }

    @Test
    fun does_not_use_email_as_display_name() {

        val session = Auth0CredentialsMapper().toSession(
            credentialsWithClaims(
                """"email":"andres@example.com""""
            )
        )

        assertEquals("Usuario", session?.user?.displayName)
        assertEquals("andres@example.com", session?.user?.email)
    }

    private fun credentialsWithClaims(claims: String): Credentials = Credentials(
        idToken = idTokenWithClaims(claims),
        accessToken = "access-token",
        type = "Bearer",
        refreshToken = null,
        expiresAt = Date(System.currentTimeMillis() + 60_000),
        scope = "openid profile email"
    )

    private fun idTokenWithClaims(claims: String): String {
        val header = encodeJwtPart("""{"alg":"none"}""")
        val payload = encodeJwtPart("""{"sub":"auth0|123",$claims}""")
        return "$header.$payload."
    }

    private fun encodeJwtPart(value: String): String =
        Base64.encodeToString(
            value.toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
}

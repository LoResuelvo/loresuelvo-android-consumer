package com.loresuelvo.consumer.data.auth

import com.auth0.android.result.Credentials
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import javax.inject.Inject
import org.json.JSONObject

/**
 * Decodes the `id_token` JWT in an Auth0 [Credentials] blob into an
 * [AuthSession]. Pure Kotlin — no Android, no Auth0 SDK at runtime,
 * no `Context`.
 *
 * Used by `Auth0AuthProvider` and tested via
 * `Auth0CredentialsMapperTest` (a pure JVM unit test that
 * hand-crafts a JWT and decodes the payload).
 *
 * `@Inject` so the Auth0 SDK wiring (which lives behind
 * `Auth0SdkWebAuthLauncher`) can pass the mapper in without manual
 * construction at the call site.
 */
class Auth0CredentialsMapper @Inject constructor() {

    fun toSession(credentials: Credentials): AuthSession? {
        val claims = decodeJwtClaims(credentials.idToken) ?: return null
        val sub = claims.optString("sub").takeIf { it.isNotBlank() } ?: return null
        val email = claims.optString("email").takeIf { it.isNotBlank() }
        val givenName = claims.optString("given_name").takeIf { it.isNotBlank() }
        val familyName = claims.optString("family_name").takeIf { it.isNotBlank() }
        val displayName = claims.optString("name").takeIf { it.isNotBlank() }
            ?: listOfNotNull(givenName, familyName).joinToString(" ")
        return AuthSession(
            user = User(
                displayName = displayName,
                firstName = givenName,
                lastName = familyName,
                email = email,
            ),
            accessToken = credentials.accessToken,
        )
    }

    private fun decodeJwtClaims(jwt: String): JSONObject? {
        val parts = jwt.split('.')
        if (parts.size < 2) return null
        return runCatching {
            val decoded = android.util.Base64.decode(
                parts[1],
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
            )
            JSONObject(String(decoded, Charsets.UTF_8))
        }.getOrNull()
    }
}

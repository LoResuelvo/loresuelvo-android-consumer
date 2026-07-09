package com.loresuelvo.consumer.data.api

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp [Authenticator] invoked when the server returns 401. The
 * current policy is "do not retry": the API does not expose a
 * refresh-token endpoint (verified against
 * `loresuelvo-api/openapi/components/security-schemes.yaml` and
 * the auth middleware in `internal/adapters/http/middleware/auth.go`),
 * so a 401 from the API is terminal for this session. The matching
 * use case translates [com.loresuelvo.consumer.domain.api.ApiError.Unauthorized]
 * into `UserRegistrationOutcome.Failure.Unauthorized` and clears
 * the local session, returning the user to the Welcome screen.
 *
 * When/if the API ships a refresh endpoint, replace `null` with a
 * request that carries the refreshed bearer token and update
 * [RetryOn401AuthenticatorTest] to assert a retry.
 */
@Singleton
class RetryOn401Authenticator @Inject constructor() : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? = null
}

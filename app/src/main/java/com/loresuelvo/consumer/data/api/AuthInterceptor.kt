package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds `Authorization: Bearer <accessToken>` to every outbound
 * request when a session is present in [AuthSessionStore]. The token
 * comes from the cached Auth0 session, not from a network call, so
 * the interceptor is safe to use on every request.
 *
 * If [AuthSessionStore.getSession] returns null or a session with a
 * blank token, the request is forwarded unchanged. The 401 retry
 * path is owned by [RetryOn401Authenticator] and the Auth0 SDK
 * (which refreshes the token internally before the request is
 * retried by the authenticator).
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authSessionStore: AuthSessionStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = authSessionStore.getSession()?.accessToken
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
                .build()
        } else {
            original
        }
        return chain.proceed(request)
    }

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}

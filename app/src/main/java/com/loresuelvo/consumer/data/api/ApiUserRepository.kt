package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.mapper.toDto
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of the [com.loresuelvo.consumer.domain.auth.UserRepository]
 * port. Adapts the [BackendApi] (Retrofit-typed) to the domain's
 * [UserRegistrationOutcome] hierarchy.
 *
 * The mapping is exhaustive: every documented backend status code
 * in `openapi/paths/consumers.yaml` has a matching outcome. The
 * repository never throws on HTTP / network failures; everything
 * becomes a typed outcome so use cases and ViewModels can handle
 * each branch explicitly.
 *
 * The session's stored [com.loresuelvo.consumer.domain.auth.User] is
 * the source of truth for the returned
 * [UserRegistrationOutcome.Success.user]. The backend does not return
 * the persisted user on `POST /consumers`; the data we have is the
 * form input + the cached session, and that is what callers receive.
 */
@Singleton
class ApiUserRepository @Inject constructor(
    private val backendApi: BackendApi,
    private val sessionStore: AuthSessionStore,
    @Suppress("unused") private val json: Json,
) : com.loresuelvo.consumer.domain.auth.UserRepository {

    override suspend fun registerConsumer(
        data: RegisterConsumerData,
    ): UserRegistrationOutcome {
        val session = sessionStore.getSession()
            ?: return UserRegistrationOutcome.Failure.Unauthorized("No active session")

        return try {
            // Returns MessageResponseDto on 2xx. Any non-2xx throws
            // HttpException, mapped via toApiError(). Network errors
            // throw IOException, also mapped.
            backendApi.registerConsumer(data.toDto())
            UserRegistrationOutcome.Success(
                session.user.copy(
                    firstName = data.firstName,
                    lastName = data.lastName,
                )
            )
        } catch (e: Throwable) {
            mapToFailure(e)
        }
    }

    private fun mapToFailure(e: Throwable): UserRegistrationOutcome.Failure =
        when (val error = e.toApiError()) {
            is com.loresuelvo.consumer.domain.api.ApiError.Network ->
                UserRegistrationOutcome.Failure.Network(error.networkCause)
            is com.loresuelvo.consumer.domain.api.ApiError.Unauthorized ->
                UserRegistrationOutcome.Failure.Unauthorized(error.errorMessage)
            is com.loresuelvo.consumer.domain.api.ApiError.Server ->
                UserRegistrationOutcome.Failure.Server(error.code, error.errorMessage)
            is com.loresuelvo.consumer.domain.api.ApiError.Unknown ->
                UserRegistrationOutcome.Failure.Server(0, error.message ?: "Unknown error")
        }
}

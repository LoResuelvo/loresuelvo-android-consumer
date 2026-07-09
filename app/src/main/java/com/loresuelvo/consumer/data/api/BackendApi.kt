package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.MessageResponseDto
import com.loresuelvo.consumer.data.api.dto.RegisterConsumerRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit-typed contract for the backend's consumer endpoints. The
 * interface is intentionally narrow: only the calls this app needs.
 * Adding a new endpoint here is the only way to add a new network
 * operation; the use cases never see Retrofit types.
 *
 * Wire paths mirror `loresuelvo-api/internal/adapters/http/router.go`
 * (registerConsumerRoutes, registerAuthenticatedRoutes).
 */
interface BackendApi {

    /**
     * `POST /consumers` — register the authenticated user as a
     * consumer. Returns a [MessageResponseDto] on 201 (the backend
     * does not return the persisted user, only an acknowledgement
     * message). All other status codes throw [retrofit2.HttpException]
     * which the data layer maps to [com.loresuelvo.consumer.domain.api.ApiError].
     */
    @POST("consumers")
    suspend fun registerConsumer(@Body body: RegisterConsumerRequestDto): MessageResponseDto
}

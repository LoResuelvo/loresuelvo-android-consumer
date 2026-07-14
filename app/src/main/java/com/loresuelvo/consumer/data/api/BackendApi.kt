package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.CategoryDto
import com.loresuelvo.consumer.data.api.dto.CurrentUserDto
import com.loresuelvo.consumer.data.api.dto.MessageResponseDto
import com.loresuelvo.consumer.data.api.dto.ProviderDto
import com.loresuelvo.consumer.data.api.dto.RegisterConsumerRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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

    @GET("me")
    suspend fun getCurrentUser(): CurrentUserDto

    /**
     * `POST /consumers` — register the authenticated user as a
     * consumer. Returns a [MessageResponseDto] on 201 (the backend
     * does not return the persisted user, only an acknowledgement
     * message). All other status codes throw [retrofit2.HttpException]
     * which the data layer maps to [com.loresuelvo.consumer.domain.api.ApiError].
     */
    @POST("consumers")
    suspend fun registerConsumer(@Body body: RegisterConsumerRequestDto): MessageResponseDto

    /**
     * `GET /categories` — the platform's service categories. Public
     * (no auth required), so it can be called before the user signs
     * in. Returns a JSON array of [CategoryDto]; non-2xx throws
     * [retrofit2.HttpException], mapped by the data layer to
     * [com.loresuelvo.consumer.domain.api.ApiError].
     */
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    /**
     * `GET /providers?category_id=X` — public service providers for
     * a category (no auth required, same as `/categories`). Returns a
     * JSON array of [ProviderDto]; non-2xx throws
     * [retrofit2.HttpException], mapped by the data layer to
     * [com.loresuelvo.consumer.domain.api.ApiError].
     */
    @GET("providers")
    suspend fun getProviders(@Query("category_id") categoryId: Int): List<ProviderDto>
}

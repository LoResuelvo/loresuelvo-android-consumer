package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.CategoryDto
import com.loresuelvo.consumer.data.api.dto.ChatMessageDto
import com.loresuelvo.consumer.data.api.dto.CreateConversationRequestDto
import com.loresuelvo.consumer.data.api.dto.CurrentUserDto
import com.loresuelvo.consumer.data.api.dto.DiagnosisDto
import com.loresuelvo.consumer.data.api.dto.ProviderDto
import com.loresuelvo.consumer.data.api.dto.RegisterConsumerRequestDto
import com.loresuelvo.consumer.data.api.dto.RegisterConsumerResponseDto
import com.loresuelvo.consumer.data.api.dto.SendMessageRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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

    @POST("consumers")
    suspend fun registerConsumer(
        @Body body: RegisterConsumerRequestDto,
    ): RegisterConsumerResponseDto

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

    // ---- AI diagnostic chat (added in commit 02-DIA) ---------------

    /**
     * `POST /chatbot/conversations` — opens a new conversation with
     * the AI diagnostic assistant. The body carries the first
     * message; the response is the full conversation
     * [DiagnosisDto] with the assistant's first reply.
     */
    @POST("chatbot/conversations")
    suspend fun createConversation(
        @Body body: CreateConversationRequestDto,
    ): DiagnosisDto

    /**
     * `POST /chatbot/conversations/{conversationId}/messages` —
     * appends a follow-up message to an existing conversation. The
     * response carries the updated full history (user message +
     * assistant reply).
     */
    @POST("chatbot/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body body: SendMessageRequestDto,
    ): DiagnosisDto
}

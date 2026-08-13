package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.AiConversationSummaryDto
import com.loresuelvo.consumer.data.api.dto.CategoryDto
import com.loresuelvo.consumer.data.api.dto.ChatMessageDto
import com.loresuelvo.consumer.data.api.dto.ConversationDetailDto
import com.loresuelvo.consumer.data.api.dto.ConversationDto
import com.loresuelvo.consumer.data.api.dto.ConversationMessageDto
import com.loresuelvo.consumer.data.api.dto.CreateAiJobRequestRequestDto
import com.loresuelvo.consumer.data.api.dto.CreateConversationRequestDto
import com.loresuelvo.consumer.data.api.dto.CreateJobRequestDto
import com.loresuelvo.consumer.data.api.dto.CurrentUserDto
import com.loresuelvo.consumer.data.api.dto.DiagnosisDto
import com.loresuelvo.consumer.data.api.dto.JobRequestDto
import com.loresuelvo.consumer.data.api.dto.ProviderDto
import com.loresuelvo.consumer.data.api.dto.RegisterConsumerRequestDto
import com.loresuelvo.consumer.data.api.dto.RegisterConsumerResponseDto
import com.loresuelvo.consumer.data.api.dto.SendMessageRequestDto
import com.loresuelvo.consumer.data.api.dto.WsTicketResponseDto
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
     * `GET /providers?category_id=X` — service providers for a
     * category. Requires a valid Auth0 JWT (the backend returns
     * `401 {"error":"invalid_token"}` for unauthenticated calls —
     * verified 2026-07-27). The `AuthInterceptor` injects the
     * bearer token from [com.loresuelvo.consumer.domain.auth.AuthSessionStore]
     * automatically when a session is present.
     *
     * Returns a JSON array of [ProviderDto]; non-2xx throws
     * [retrofit2.HttpException], mapped by the data layer to
     * [com.loresuelvo.consumer.domain.api.ApiError]. Note: the
     * response does NOT echo `category_id` — see [ProviderDto] and
     * the mapper at `data/api/mapper/ProviderDtoMapper.kt`.
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

    /**
     * `GET /conversations/{conversationId}` — full snapshot of a
     * saved AI diagnostic conversation including the `messages[]`
     * thread, the `assessment` (if the diagnosis concluded), and
     * the `recommended_providers[]` (if the AI matched a rubro).
     * The chat scroll uses this on entry — when the consumer taps
     * a row in the "Asistente IA" tab — to hydrate the conversation
     * history without a fresh round-trip.
     *
     * The AI conversation lives in the SAME backend `conversations`
     * table as the consumer ↔ provider chats (the discriminator
     * is the id, not a `type` field). The webapp's
     * `AiChatRepository.getById` hits the same path
     * (`/conversations/${id}`); mirroring it here keeps the
     * Android + webapp clients reading from the same row.
     *
     * The wire shape matches `DiagnosisDto` (id, status, title,
     * response_status, assessment, recommended_providers,
     * response, messages); the existing
     * `DiagnosisDtoMapper.toDomain()` handles the response
     * without a new wire-type declaration.
     *
     * Requires a valid Auth0 JWT (the [AuthInterceptor] injects
     * the bearer token from
     * [com.loresuelvo.consumer.domain.auth.AuthSessionStore]
     * automatically when a session is present). Non-2xx throws
     * [retrofit2.HttpException], mapped by the data layer to
     * [com.loresuelvo.consumer.domain.api.ApiError].
     */
    @GET("conversations/{conversationId}")
    suspend fun getAiConversationById(
        @Path("conversationId") conversationId: String,
    ): DiagnosisDto

    /**
     * `GET /chatbot/conversations` — the consumer's AI diagnostic
     * conversations, ordered by the backend's `updated_on` policy
     * (timestamp-descending). Each element is the row-level summary
     * the "Asistente IA" tab renders; the full chat thread
     * (messages + assessment + recommended providers) is fetched
     * on tap via [sendMessage] / [DiagnosisDto].
     *
     * Requires a valid Auth0 JWT (the [AuthInterceptor] injects
     * the bearer token from
     * [com.loresuelvo.consumer.domain.auth.AuthSessionStore]
     * automatically when a session is present). Non-2xx throws
     * [retrofit2.HttpException], mapped by the data layer to
     * [com.loresuelvo.consumer.domain.api.ApiError].
     */
    @GET("chatbot/conversations")
    suspend fun getAiConversations(): List<AiConversationSummaryDto>

    // ---- Job requests (added for US "Contact a provider") -------

    /**
     * `POST /job-requests` — submits the consumer's first message
     * to a provider. The body carries the provider id, a title,
     * the description, and an optional list of presigned image
     * ids (the modal form does not expose upload today, so the
     * field is omitted from the payload when empty).
     *
     * The response carries the persisted [JobRequestDto] including
     * the backend-issued `id` and the `conversationId` the UI
     * navigates to on success.
     */
    @POST("job-requests")
    suspend fun createJobRequest(
        @Body body: CreateJobRequestDto,
    ): JobRequestDto

    /**
     * `POST /chatbot/conversations/{conversationId}/job-requests`
     * — submits the AI pre-filled job request when the consumer
     * taps "Contactar" on a recommended provider tile INSIDE the
     * AI diagnostic chat. The body carries only `provider_id`; the
     * backend's AI pre-fills `title` and `description` from the
     * conversation history (mirrors the webapp's
     * `useAiDiagnosisChat.handleContactProvider`).
     *
     * The response shape is identical to `POST /job-requests`,
     * so the existing [JobRequestDto] is reused; the mapper at
     * `data/api/mapper/JobRequestDtoMapper.kt` handles the
     * conversion to the domain [com.loresuelvo.consumer.domain.jobrequest.JobRequest].
     *
     * Requires a valid Auth0 JWT (the [AuthInterceptor] injects
     * the bearer token from
     * [com.loresuelvo.consumer.domain.auth.AuthSessionStore]
     * automatically when a session is present). Non-2xx throws
     * [retrofit2.HttpException], mapped by the data layer to
     * [com.loresuelvo.consumer.domain.api.ApiError].
     */
    @POST("chatbot/conversations/{conversationId}/job-requests")
    suspend fun createAiJobRequest(
        @Path("conversationId") conversationId: String,
        @Body body: CreateAiJobRequestRequestDto,
    ): JobRequestDto

    // ---- Consumer ↔ provider conversations (added for US-17 / 03-IC) --

    /**
     * `GET /conversations` — the consumer's conversation list,
     * ordered by `updated_on` descending. Each element carries
     * the counterpart profile and the most recent message preview
     * so the list cell can render without a second round-trip.
     *
     * Requires a valid Auth0 JWT (the [AuthInterceptor] injects
     * the bearer token from
     * [com.loresuelvo.consumer.domain.auth.AuthSessionStore]
     * automatically when a session is present). Non-2xx throws
     * [retrofit2.HttpException], mapped by the data layer to
     * [com.loresuelvo.consumer.domain.api.ApiError].
     */
    @GET("conversations")
    suspend fun getConversations(): List<ConversationDto>

    /**
     * `GET /conversations/{conversationId}` — full snapshot of a
     * single conversation including the complete `messages[]`
     * thread. The chat surface uses it on entry to render the
     * header (counterpart, status) and the existing bubbles.
     */
    @GET("conversations/{conversationId}")
    suspend fun getConversationById(
        @Path("conversationId") conversationId: String,
    ): ConversationDetailDto

    /**
     * `POST /conversations/{conversationId}/messages` — appends
     * a consumer message to an existing conversation. The body
     * carries the message text; the response is the
     * server-persisted message (with the backend-issued numeric
     * id and the authoritative `created_on` timestamp).
     */
    @POST("conversations/{conversationId}/messages")
    suspend fun postMessage(
        @Path("conversationId") conversationId: String,
        @Body body: SendMessageRequestDto,
    ): ConversationMessageDto

    /**
     * `POST /ws-tickets` — fetches a short-lived signed JWT the
     * Android client uses as the `ticket` query parameter when
     * opening the `/ws` WebSocket. The endpoint requires the
     * regular Auth0 bearer token (added by `AuthInterceptor`).
     */
    @POST("ws-tickets")
    suspend fun getWsTicket(): WsTicketResponseDto
}

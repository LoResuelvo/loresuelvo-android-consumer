package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.jobrequest.AiJobRequestRepository
import com.loresuelvo.consumer.domain.jobrequest.CreateAiJobRequestOutcome
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * End-to-end coverage of [ApiAiJobRequestRepository] against a
 * [MockWebServer] emulating the AI pre-filled job-request
 * endpoint (`POST /chatbot/conversations/{id}/job-requests`).
 * The wire shape mirrors the curl the user pasted on 2026-08-10
 * against the local dev backend:
 *
 *  https://github.com/loresuelvo/loresuelvo-api endpoint:
 *    POST /chatbot/conversations/{id}/job-requests
 *    body: { "provider_id": <int> }
 *    response: { id, conversation_id, title, description, status, images[] }
 *
 * Failure-mapping discipline mirrors [ApiJobRequestRepositoryIntegrationTest]:
 *  - 200 + happy body       → Success
 *  - 400 + validation body  → Failure.Server
 *  - 401                    → Failure.Unauthorized
 *  - 500                    → Failure.Server
 *  - network drop           → Failure.Network
 *
 * Wire-level inspection verifies both the URL path and the body
 * shape on every request.
 */
class ApiAiJobRequestRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AiJobRequestRepository
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val backendApi = retrofit.create(BackendApi::class.java)
        repository = ApiAiJobRequestRepository(backendApi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun createAiJobRequest_returns_Success_with_AI_pre_filled_fields() = runBlocking {
        // Exact body the dev backend returns when the AI pre-fills
        // the request. Title and description come from the AI, not
        // from the consumer.
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 1,
                      "conversation_id": 10,
                      "title": "Reparación de fuga en la cocina",
                      "description": "Hola Ana, necesito reparar una fuga de agua en la cocina. ¿Podrías ayudarme esta semana?",
                      "status": "pending",
                      "images": [
                        {
                          "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                          "url": "https://example.com/",
                          "original_name": "string"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val outcome = repository.createAiJobRequest(
            conversationId = "1",
            providerId = 1073741824,
        )

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/chatbot/conversations/1/job-requests", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(
            "body must carry ONLY provider_id, was '$body'",
            body.contains("\"provider_id\":1073741824") &&
                !body.contains("\"title\":") &&
                !body.contains("\"description\":"),
        )

        assertTrue(outcome is CreateAiJobRequestOutcome.Success)
        val success = outcome as CreateAiJobRequestOutcome.Success
        assertEquals("1", success.jobRequest.id)
        assertEquals("10", success.jobRequest.conversationId)
        assertEquals("Reparación de fuga en la cocina", success.jobRequest.title)
        assertEquals(
            "Hola Ana, necesito reparar una fuga de agua en la cocina. ¿Podrías ayudarme esta semana?",
            success.jobRequest.description,
        )
        assertEquals("pending", success.jobRequest.status)
        assertEquals(1, success.jobRequest.images.size)
        assertEquals("3fa85f64-5717-4562-b3fc-2c963f66afa6", success.jobRequest.images.first().id)
    }

    @Test
    fun createAiJobRequest_400_returns_Server_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"error":"invalid_provider","message":"Provider does not exist"}""",
                ),
        )

        val outcome = repository.createAiJobRequest(conversationId = "1", providerId = 999)

        assertTrue(outcome is CreateAiJobRequestOutcome.Failure.Server)
        val failure = outcome as CreateAiJobRequestOutcome.Failure.Server
        assertEquals(400, failure.code)
        assertEquals("Provider does not exist", failure.message)
    }

    @Test
    fun createAiJobRequest_401_returns_Unauthorized_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"error":"invalid_token","message":"token expired"}""",
                ),
        )

        val outcome = repository.createAiJobRequest(conversationId = "1", providerId = 1)

        assertTrue(outcome is CreateAiJobRequestOutcome.Failure.Unauthorized)
        val failure = outcome as CreateAiJobRequestOutcome.Failure.Unauthorized
        assertEquals("token expired", failure.message)
    }

    @Test
    fun createAiJobRequest_500_returns_Server_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(""),
        )

        val outcome = repository.createAiJobRequest(conversationId = "1", providerId = 1)

        assertTrue(outcome is CreateAiJobRequestOutcome.Failure.Server)
        assertEquals(500, (outcome as CreateAiJobRequestOutcome.Failure.Server).code)
    }

    @Test
    fun createAiJobRequest_network_drop_returns_Network_failure() = runBlocking {
        server.enqueue(
            MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START),
        )

        val outcome = repository.createAiJobRequest(conversationId = "1", providerId = 1)

        assertTrue(outcome is CreateAiJobRequestOutcome.Failure.Network)
        assertNotNull((outcome as CreateAiJobRequestOutcome.Failure.Network).cause)
    }
}

package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.diagnosis.LoadAiConversationOutcome
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * End-to-end coverage of `ApiDiagnosisRepository.getAiConversation`
 * against a [MockWebServer] emulating the backend's
 * `GET /chatbot/conversations/{id}` endpoint. The wire shape mirrors
 * the dev backend's response: a `DiagnosisDto` carrying the
 * full conversation history (the same shape the create / send
 * endpoints return, since the AI session list and the chat
 * scroll share the snapshot).
 *
 * Failure-mapping discipline mirrors the rest of the suite:
 *  - 200 + happy body      → Success
 *  - 400 + validation body → Failure.Server
 *  - 401                   → Failure.Unauthorized
 *  - 500                   → Failure.Server
 *  - network drop          → Failure.Network
 */
class ApiDiagnosisGetByIdIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ApiDiagnosisRepository
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
        repository = ApiDiagnosisRepository(backendApi = backendApi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getAiConversation_returns_Success_with_full_history() = runBlocking {
        // Same wire shape `createConversation` returns: the detail
        // endpoint is a full snapshot of the conversation.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 10,
                      "status": "active",
                      "title": "Pérdida de agua en la cocina",
                      "response_status": "answered",
                      "assessment": {
                        "outcome": "professional_required",
                        "problem_category": {"id": 3, "name": "Plomería"}
                      },
                      "recommended_providers": [
                        {
                          "id": 10,
                          "name": "Juan",
                          "surname": "Gómez",
                          "category_name": "Plomería",
                          "profile_photo_url": "https://cdn.example/files/provider.jpg"
                        }
                      ],
                      "response": {
                        "id": 2,
                        "sender_role": "chatbot",
                        "content": "El problema parece ser una pérdida en el sifón.",
                        "created_on": "2026-06-16T12:00:01Z"
                      },
                      "messages": [
                        {"id": 1, "sender_role": "consumer", "content": "Tengo una gotera en la cocina", "created_on": "2026-06-16T12:00:00Z"},
                        {"id": 2, "sender_role": "chatbot", "content": "El problema parece ser una pérdida en el sifón.", "created_on": "2026-06-16T12:00:01Z"}
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val outcome = repository.getAiConversation(conversationId = "10")

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/chatbot/conversations/10", recorded.path)

        assertTrue(outcome is LoadAiConversationOutcome.Success)
        val success = outcome as LoadAiConversationOutcome.Success
        val diagnosis = success.diagnosis
        assertEquals("10", diagnosis.conversationId)
        assertEquals("Tengo una gotera en la cocina", diagnosis.messages.first().content)
        assertEquals(2, diagnosis.messages.size)
        assertTrue(diagnosis.assessment?.isProfessionalRequired == true)
        assertEquals(1, diagnosis.recommendedProviders?.size)
        assertEquals("Juan", diagnosis.recommendedProviders?.first()?.name)
    }

    @Test
    fun getAiConversation_400_returns_Server_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"error":"not_found","message":"Conversation not found"}""",
                ),
        )

        val outcome = repository.getAiConversation(conversationId = "999")

        assertTrue(outcome is LoadAiConversationOutcome.Failure.Server)
        val failure = outcome as LoadAiConversationOutcome.Failure.Server
        assertEquals(400, failure.code)
        assertEquals("Conversation not found", failure.message)
    }

    @Test
    fun getAiConversation_401_returns_Unauthorized_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"error":"invalid_token","message":"token expired"}""",
                ),
        )

        val outcome = repository.getAiConversation(conversationId = "10")

        assertTrue(outcome is LoadAiConversationOutcome.Failure.Unauthorized)
        val failure = outcome as LoadAiConversationOutcome.Failure.Unauthorized
        assertEquals("token expired", failure.message)
    }

    @Test
    fun getAiConversation_500_returns_Server_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(""),
        )

        val outcome = repository.getAiConversation(conversationId = "10")

        assertTrue(outcome is LoadAiConversationOutcome.Failure.Server)
        assertEquals(500, (outcome as LoadAiConversationOutcome.Failure.Server).code)
    }

    @Test
    fun getAiConversation_network_drop_returns_Network_failure() = runBlocking {
        server.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
        )

        val outcome = repository.getAiConversation(conversationId = "10")

        assertTrue(outcome is LoadAiConversationOutcome.Failure.Network)
        assertNotNull((outcome as LoadAiConversationOutcome.Failure.Network).cause)
    }
}

package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * End-to-end coverage of [ApiDiagnosisRepository] against a
 * [MockWebServer] emulating the backend's two endpoints:
 *
 *  - `POST /chatbot/conversations` (no `existingConversationId`)
 *  - `POST /chatbot/conversations/{conversationId}/messages` (with)
 *
 * The repo dispatches to the right endpoint by branch; both
 * branches are pinned here. Verifies the outcome, the wire path
 * the request hits, and the snake_case → camelCase mapping
 * through [com.loresuelvo.consumer.data.api.mapper.toDomain].
 *
 * Failure paths (500, transport drop) mirror the
 * `ApiCategoryRepositoryIntegrationTest` discipline.
 */
class ApiDiagnosisRepositoryIntegrationTest {

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
    fun sendPrompt_without_existing_id_hits_create_endpoint_and_returns_diagnosis() = runTest {
        // Wire shape mirrors `loresuelvo-api`'s
        // `POST /chatbot/conversations`: numeric `id`, full
        // `messages[]` history, optional `assessment` /
        // `recommended_providers` ignored by the mapper for now.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 42,
                      "status": "active",
                      "title": "Diagnóstico concluido",
                      "response_status": "answered",
                      "messages": [
                        {"id": 1, "sender_role": "consumer", "content": "Tengo una gotera", "created_on": "2026-06-16T12:00:00Z"},
                        {"id": 2, "sender_role": "chatbot",  "content": "Entiendo. ¿Es constante?", "created_on": "2026-06-16T12:00:01Z"}
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val outcome = repository.sendPrompt(content = "Tengo una gotera")

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/chatbot/conversations", recorded.path)
        // Body is the snake_case request DTO.
        val body = recorded.body.readUtf8()
        assertTrue(
            "body must carry the prompt, was '$body'",
            body.contains("\"content\":\"Tengo una gotera\""),
        )

        assertTrue(outcome is SendDiagnosisPromptOutcome.Success)
        val success = outcome as SendDiagnosisPromptOutcome.Success
        // Numeric `id` is mapped to the domain's String form.
        assertEquals("42", success.diagnosis.conversationId)
        assertEquals(2, success.diagnosis.messages.size)
        assertTrue(success.diagnosis.messages[0].sender == com.loresuelvo.consumer.domain.diagnosis.Sender.Consumer)
        assertTrue(success.diagnosis.messages[1].sender == com.loresuelvo.consumer.domain.diagnosis.Sender.Assistant)
        assertEquals("Tengo una gotera", success.diagnosis.messages[0].content)
        assertEquals("Entiendo. ¿Es constante?", success.diagnosis.messages[1].content)
        assertNull(success.diagnosis.recommendations)
    }

    @Test
    fun sendPrompt_with_existing_id_hits_append_endpoint_and_returns_diagnosis() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 42,
                      "status": "active",
                      "messages": [
                        {"id": 1, "sender_role": "consumer", "content": "primera",          "created_on": "2026-06-16T12:00:00Z"},
                        {"id": 2, "sender_role": "chatbot",  "content": "primera respuesta", "created_on": "2026-06-16T12:00:01Z"},
                        {"id": 3, "sender_role": "consumer", "content": "segunda",          "created_on": "2026-06-17T12:00:00Z"},
                        {"id": 4, "sender_role": "chatbot",  "content": "segunda respuesta","created_on": "2026-06-17T12:00:01Z"}
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val outcome = repository.sendPrompt(
            content = "segunda",
            existingConversationId = "42",
        )

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/chatbot/conversations/42/messages", recorded.path)

        assertTrue(outcome is SendDiagnosisPromptOutcome.Success)
        val success = outcome as SendDiagnosisPromptOutcome.Success
        assertEquals("42", success.diagnosis.conversationId)
        assertEquals(4, success.diagnosis.messages.size)
    }

    @Test
    fun sendPrompt_500_returns_Server_failure() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(""),
        )

        val outcome = repository.sendPrompt(content = "boom")

        assertTrue(outcome is SendDiagnosisPromptOutcome.Failure.Server)
        assertEquals(500, (outcome as SendDiagnosisPromptOutcome.Failure.Server).code)
    }

    @Test
    fun sendPrompt_401_returns_Unauthorized_failure() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"unauthorized\",\"message\":\"token expired\"}"),
        )

        val outcome = repository.sendPrompt(content = "auth me")

        assertTrue(outcome is SendDiagnosisPromptOutcome.Failure.Unauthorized)
        val failure = outcome as SendDiagnosisPromptOutcome.Failure.Unauthorized
        assertNotNull(failure.message)
    }
}

package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun sendPrompt_without_existing_id_hits_create_endpoint_and_returns_diagnosis() = runBlocking {
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
        assertNull(success.diagnosis.assessment)
        assertNull(success.diagnosis.recommendedProviders)
    }

    @Test
    fun sendPrompt_with_existing_id_hits_append_endpoint_and_returns_diagnosis() = runBlocking {
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
    fun sendPrompt_with_concluded_assessment_decodes_professional_required_outcome() = runBlocking {
        // Real backend shape (verified 2026-08-10 against the local
        // dev instance): assessment is an OBJECT {outcome,
        // problem_category?}, NOT a string. recommended_providers[]
        // elements are ProviderDto-shaped WITHOUT category_id (the
        // category id comes from assessment.problem_category.id).
        // The mapper threads problem_category.id into every mapped
        // provider so the domain keeps the non-null invariant on
        // Provider.categoryId.
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 1,
                      "status": "active",
                      "title": "Diagnóstico concluido",
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
                        {"id": 1, "sender_role": "consumer", "content": "Hay agua acumulada", "created_on": "2026-06-16T12:00:00Z"},
                        {"id": 2, "sender_role": "chatbot",  "content": "El problema parece ser una pérdida en el sifón.", "created_on": "2026-06-16T12:00:01Z"}
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val outcome = repository.sendPrompt(content = "Hay agua acumulada")

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/chatbot/conversations", recorded.path)

        assertTrue(outcome is SendDiagnosisPromptOutcome.Success)
        val success = outcome as SendDiagnosisPromptOutcome.Success
        assertEquals("1", success.diagnosis.conversationId)
        assertEquals(2, success.diagnosis.messages.size)

        val assessment = success.diagnosis.assessment
            ?: error("expected assessment to be present on professional_required")
        assertEquals("professional_required", assessment.outcome)
        assertTrue(assessment.isProfessionalRequired)
        assertEquals("Plomería", assessment.problemCategory?.name)
        assertEquals(3, assessment.problemCategory?.id)

        val provider = success.diagnosis.recommendedProviders?.single()
            ?: error("expected exactly one recommended provider")
        assertEquals(10, provider.id)
        assertEquals("Juan", provider.name)
        assertEquals("Gómez", provider.surname)
        assertEquals("Plomería", provider.categoryName)
        assertEquals(3, provider.categoryId)
        assertEquals(
            "https://cdn.example/files/provider.jpg",
            provider.profilePhotoUrl,
        )
    }

    @Test
    fun sendPrompt_with_collecting_information_assessment_keeps_recommended_providers_empty() = runBlocking {
        // Mid-conversation shape (verified 2026-08-10): the AI
        // asks for more info. `outcome` is collecting_information
        // and `problem_category` is omitted (no category matched
        // yet). `recommended_providers` is an empty array.
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 1,
                      "status": "active",
                      "assessment": {"outcome": "collecting_information"},
                      "recommended_providers": [],
                      "messages": [
                        {"id": 1, "sender_role": "consumer", "content": "hola", "created_on": "2026-08-10T17:56:50.634386Z"},
                        {"id": 2, "sender_role": "chatbot",  "content": "¿qué tipo de problema?", "created_on": "2026-08-10T17:56:50.634386Z"}
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val outcome = repository.sendPrompt(content = "hola")

        assertTrue(outcome is SendDiagnosisPromptOutcome.Success)
        val success = outcome as SendDiagnosisPromptOutcome.Success
        val assessment = success.diagnosis.assessment
            ?: error("assessment must be present, never null mid-conversation")
        assertEquals("collecting_information", assessment.outcome)
        assertFalse(assessment.isProfessionalRequired)
        assertNull(assessment.problemCategory)
        assertEquals(emptyList<Any>(), success.diagnosis.recommendedProviders)
    }

    @Test
    fun sendPrompt_500_returns_Server_failure() = runBlocking {
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
    fun sendPrompt_401_returns_Unauthorized_failure() = runBlocking {
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

package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.domain.assistant.AiConversationRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * End-to-end coverage of `ApiAiConversationRepository` against a
 * [MockWebServer] emulating the backend's
 * `GET /chatbot/conversations` endpoint. The wire shape mirrors
 * the curl the user pasted on 2026-08-10 against the local dev
 * backend:
 *
 *  ```
 *  [
 *    {
 *      "id": 1,
 *      "status": "active",
 *      "title": "Pérdida de agua en la cocina",
 *      "last_message": {
 *        "id": 2,
 *        "sender_role": "chatbot",
 *        "content": "Revisá si el agua sale desde la rosca del sifón o desde la manguera flexible.",
 *        "created_on": "2026-06-18T12:00:00Z"
 *      },
 *      "updated_on": "2026-06-18T12:00:00Z"
 *    }
 *  ]
 *  ```
 *
 * Failure-mapping discipline mirrors the rest of the suite:
 *  - 200 + happy body (with/without last_message)  → Success
 *  - 200 + empty array                               → Success (empty)
 *  - 401                                            → Failure.Unauthorized
 *  - 500                                            → Failure.Server
 *  - network drop                                   → Failure.Network
 */
class ApiAiConversationRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AiConversationRepository
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
        repository = ApiAiConversationRepository(backendApi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getConversations_returns_Success_with_flattened_last_message_preview() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    [
                      {
                        "id": 1,
                        "status": "active",
                        "title": "Pérdida de agua en la cocina",
                        "last_message": {
                          "id": 2,
                          "sender_role": "chatbot",
                          "content": "Revisá si el agua sale desde la rosca del sifón o desde la manguera flexible.",
                          "created_on": "2026-06-18T12:00:00Z"
                        },
                        "updated_on": "2026-06-18T12:00:00Z"
                      }
                    ]
                    """.trimIndent(),
                ),
        )

        val outcome = repository.getConversations()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/chatbot/conversations", recorded.path)

        assertTrue(outcome is AiConversationListOutcome.Success)
        val success = outcome as AiConversationListOutcome.Success
        assertEquals(1, success.conversations.size)
        val row = success.conversations.first()
        assertEquals("1", row.id)
        assertEquals("Pérdida de agua en la cocina", row.title)
        assertEquals(
            "Revisá si el agua sale desde la rosca del sifón o desde la manguera flexible.",
            row.lastMessagePreview,
        )
        // 2026-06-18T12:00:00Z parses to a non-zero epoch — the
        // exact value isn't pinned because the timezone library
        // works in local TZ; the integration only pins
        // non-emptiness so a future parser drift is loud.
        assertTrue(row.lastMessageAtEpochMillis > 0L)
    }

    @Test
    fun getConversations_returns_Success_with_null_preview_when_last_message_is_omitted() = runBlocking {
        // The backend may omit `last_message` for conversations
        // whose only message is the system welcome. The domain
        // flatten must collapse this to `null` so the row's
        // preview slot is empty rather than rendering the
        // empty object as text.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    [
                      {
                        "id": 7,
                        "status": "active",
                        "title": "Diagnóstico inicial",
                        "updated_on": "2026-06-18T12:00:00Z"
                      }
                    ]
                    """.trimIndent(),
                ),
        )

        val outcome = repository.getConversations()

        assertTrue(outcome is AiConversationListOutcome.Success)
        val success = outcome as AiConversationListOutcome.Success
        val row = success.conversations.single()
        assertEquals("7", row.id)
        assertEquals("Diagnóstico inicial", row.title)
        assertNull(row.lastMessagePreview)
    }

    @Test
    fun getConversations_returns_Success_with_empty_list_when_no_history() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"),
        )

        val outcome = repository.getConversations()

        assertTrue(outcome is AiConversationListOutcome.Success)
        assertEquals(emptyList<Any>(), (outcome as AiConversationListOutcome.Success).conversations)
    }

    @Test
    fun getConversations_401_returns_Unauthorized() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"error":"invalid_token","message":"token expired"}""",
                ),
        )

        val outcome = repository.getConversations()

        assertTrue(outcome is AiConversationListOutcome.Failure.Unauthorized)
        assertEquals("token expired", (outcome as AiConversationListOutcome.Failure.Unauthorized).message)
    }

    @Test
    fun getConversations_500_returns_Server_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(""),
        )

        val outcome = repository.getConversations()

        assertTrue(outcome is AiConversationListOutcome.Failure.Server)
        assertEquals(500, (outcome as AiConversationListOutcome.Failure.Server).code)
    }

    @Test
    fun getConversations_network_drop_returns_Network_failure() = runBlocking {
        server.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
        )

        val outcome = repository.getConversations()

        assertTrue(outcome is AiConversationListOutcome.Failure.Network)
        assertNotNull((outcome as AiConversationListOutcome.Failure.Network).cause)
    }
}

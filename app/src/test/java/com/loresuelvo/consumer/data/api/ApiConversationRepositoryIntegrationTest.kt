package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
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
 * End-to-end coverage of [ApiConversationRepository] against a
 * [MockWebServer] emulating `GET /conversations`.
 *
 * Mirrors the discipline of [ApiDiagnosisRepositoryIntegrationTest]:
 *  - pin the wire path the request hits;
 *  - pin the snake_case → camelCase mapping through
 *    [com.loresuelvo.consumer.data.api.mapper.toDomain];
 *  - cover the success / empty / 500 / 401 / transport-drop
 *    branches so the failure-mapping discipline stays in sync
 *    with the other repositories.
 */
class ApiConversationRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ApiConversationRepository
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
        repository = ApiConversationRepository(backendApi = backendApi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getConversations_hits_endpoint_and_maps_payload() = runBlocking {
        // Two entries: one with a last_message, one without. The
        // mapper must tolerate both shapes (last_message is
        // nullable in the domain).
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    [
                      {
                        "id": 1,
                        "status": "pending",
                        "counterpart": {
                          "id": 20,
                          "role": "provider",
                          "name": "Juan",
                          "surname": "Gómez",
                          "category_name": "Plomería",
                          "profile_photo_url": "https://cdn.example/juan.jpg"
                        },
                        "last_message": {
                          "id": 1,
                          "sender_role": "consumer",
                          "content": "Hola Juan, necesito reparar una pérdida de agua en la cocina. ¿Podrías ayudarme esta semana?",
                          "created_on": "2026-05-30T14:20:00Z"
                        },
                        "updated_on": "2026-05-30T14:20:00Z"
                      },
                      {
                        "id": 2,
                        "status": "pending",
                        "counterpart": {
                          "id": 30,
                          "role": "provider",
                          "name": "Pedro",
                          "surname": "Dib",
                          "category_name": "Plomería"
                        },
                        "updated_on": "2026-05-29T10:00:00Z"
                      }
                    ]
                    """.trimIndent(),
                ),
        )

        val outcome = repository.getConversations()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/conversations", recorded.path)

        assertTrue(outcome is ConversationsOutcome.Success)
        val success = outcome as ConversationsOutcome.Success
        assertEquals(2, success.conversations.size)

        // First entry: full payload with last_message + photo URL.
        val first: Conversation = success.conversations[0]
        assertEquals("1", first.id)
        assertEquals(ConversationStatus.Pending, first.status)
        assertEquals(
            ConversationCounterpart(
                id = 20L,
                name = "Juan",
                surname = "Gómez",
                categoryName = "Plomería",
                profilePhotoUrl = "https://cdn.example/juan.jpg",
            ),
            first.counterpart,
        )
        assertNotNull(first.lastMessage)
        val lastMessage = first.lastMessage!!
        assertEquals("1", lastMessage.id)
        assertEquals(ConversationSender.Consumer, lastMessage.sender)
        assertTrue(lastMessage.createdOnEpochMillis != 0L)
        assertTrue(first.updatedOnEpochMillis != 0L)

        // Second entry: no last_message, no photo URL.
        val second = success.conversations[1]
        assertEquals("2", second.id)
        assertNull(second.lastMessage)
        assertNull(second.counterpart.profilePhotoUrl)
        assertTrue(second.updatedOnEpochMillis != 0L)
    }

    @Test
    fun getConversations_with_empty_array_returns_Success_with_empty_list() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"),
        )

        val outcome = repository.getConversations()

        assertTrue(outcome is ConversationsOutcome.Success)
        assertEquals(
            emptyList<Conversation>(),
            (outcome as ConversationsOutcome.Success).conversations,
        )
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

        assertTrue(outcome is ConversationsOutcome.Failure.Server)
        assertEquals(500, (outcome as ConversationsOutcome.Failure.Server).code)
    }

    @Test
    fun getConversations_401_returns_Unauthorized_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"error":"invalid_token","message":"token expired"}""",
                ),
        )

        val outcome = repository.getConversations()

        // The conversations outcome has a dedicated Unauthorized
        // subtype (categories does not) — the adapter must map to
        // it so the VM can clear the local session.
        assertTrue(outcome is ConversationsOutcome.Failure.Unauthorized)
        assertEquals(
            "token expired",
            (outcome as ConversationsOutcome.Failure.Unauthorized).message,
        )
    }

    @Test
    fun getConversations_transport_drop_returns_Network_failure() = runBlocking {
        server.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
        )

        val outcome = repository.getConversations()

        assertTrue(outcome is ConversationsOutcome.Failure.Network)
        // lastMessage of a fresh conversation is null in the wire too
        // — covered by the success test; this one only asserts the
        // transport-failure mapping.
    }
}
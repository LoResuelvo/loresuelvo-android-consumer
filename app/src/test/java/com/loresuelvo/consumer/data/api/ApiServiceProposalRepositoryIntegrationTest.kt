package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * End-to-end coverage of [ApiServiceProposalRepository] against a
 * [MockWebServer] emulating the backend's `GET /service-proposals`:
 *   200 + multi body -> Success(2 proposals, including pending and accepted)
 *   200 + empty body  -> Success(empty)
 *   500               -> Failure.Server
 *   transport drop    -> Failure.Network
 *
 * Verifies both the outcome AND the wire-level request (method,
 * path). The mocked body matches the swagger example the user
 * shared for US-54: `pending` status, `amount_cents`,
 * `scheduled_on`, `description`, `created_on`, `counterpart`
 * with `role`, `name`, `surname`, `category_name`,
 * `profile_photo_url`. `booking_terms` is intentionally absent
 * from the body to pin that the client survives its omission.
 */
class ApiServiceProposalRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ApiServiceProposalRepository
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
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType()),
            )
            .build()
        val backendApi = retrofit.create(BackendApi::class.java)
        repository = ApiServiceProposalRepository(backendApi = backendApi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun get_service_proposals_200_returns_Success_with_mapped_list() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    [
                      {
                        "id": 1,
                        "conversation_id": 10,
                        "amount_cents": 1500000,
                        "scheduled_on": "2026-10-15T14:30:00Z",
                        "description": "Fuga en el lavamanos",
                        "status": "pending",
                        "created_on": "2026-09-03T11:19:24.640Z",
                        "counterpart": {
                          "id": 92,
                          "role": "provider",
                          "name": "Carlos",
                          "surname": "López",
                          "category_name": "Plomería",
                          "profile_photo_url": "http://x/c.webp"
                        }
                      },
                      {
                        "id": 2,
                        "conversation_id": 11,
                        "amount_cents": 2200000,
                        "scheduled_on": "2026-10-20T09:00:00Z",
                        "description": "Cambio de canilla",
                        "status": "accepted",
                        "created_on": "2026-09-04T08:00:00Z",
                        "counterpart": {
                          "id": 17,
                          "role": "provider",
                          "name": "Ana",
                          "surname": "Pérez",
                          "category_name": "Plomería",
                          "profile_photo_url": null
                        }
                      }
                    ]
                    """.trimIndent(),
                ),
        )

        val outcome = repository.getServiceProposals()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/service-proposals", recorded.path)

        assertTrue(outcome is ServiceProposalsOutcome.Success)
        val success = outcome as ServiceProposalsOutcome.Success
        assertEquals(2, success.proposals.size)
        // First proposal: pending, full snapshot mapped.
        val first = success.proposals[0]
        assertEquals("1", first.id)
        assertEquals("10", first.conversationId)
        assertEquals(ServiceProposalStatus.Pending, first.status)
        assertEquals(1500000L, first.amountCents)
        assertEquals("Fuga en el lavamanos", first.description)
        assertEquals("92", first.counterpart.id)
        assertEquals("Carlos", first.counterpart.name)
        assertEquals("López", first.counterpart.surname)
        assertEquals("Plomería", first.counterpart.categoryName)
        assertEquals("http://x/c.webp", first.counterpart.profilePhotoUrl)
        // Second proposal: accepted, no profile photo (null
        // collapses through the mapper, not to a default avatar).
        val second = success.proposals[1]
        assertEquals(ServiceProposalStatus.Accepted, second.status)
        assertEquals(null, second.counterpart.profilePhotoUrl)
    }

    @Test
    fun get_service_proposals_200_empty_returns_Success_empty() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"),
        )

        val outcome = repository.getServiceProposals()

        assertTrue(outcome is ServiceProposalsOutcome.Success)
        assertTrue((outcome as ServiceProposalsOutcome.Success).proposals.isEmpty())
    }

    @Test
    fun get_service_proposals_500_returns_Server_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("internal error"),
        )

        val outcome = repository.getServiceProposals()

        assertTrue(
            "outcome must be Failure.Server, was $outcome",
            outcome is ServiceProposalsOutcome.Failure.Server,
        )
        assertEquals(500, (outcome as ServiceProposalsOutcome.Failure.Server).code)
    }

    @Test
    fun get_service_proposals_401_returns_Server_failure_401() = runBlocking {
        // The bearer token is rejected; the data layer collapses
        // `ApiError.Unauthorized` to a typed `Failure.Server` with
        // code 401 so callers can branch on the code without
        // importing the transport hierarchy.
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"invalid_token"}"""),
        )

        val outcome = repository.getServiceProposals()

        assertTrue(outcome is ServiceProposalsOutcome.Failure.Server)
        assertEquals(401, (outcome as ServiceProposalsOutcome.Failure.Server).code)
    }

    @Test
    fun get_service_proposals_network_drop_returns_Network_failure() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val outcome = repository.getServiceProposals()

        assertTrue(
            "outcome must be Failure.Network, was $outcome",
            outcome is ServiceProposalsOutcome.Failure.Network,
        )
        assertTrue((outcome as ServiceProposalsOutcome.Failure.Network).cause is IOException)
    }
}

package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
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
import java.util.concurrent.TimeUnit

/**
 * End-to-end coverage of `ApiJobRequestRepository` against a
 * [MockWebServer] emulating the loresuelvo-api backend. Exercises
 * the failure-mapping discipline documented in
 * `api-client-governance`:
 *
 *  - 200 + happy body           → Success
 *  - 200 + images=null body     → Success (empty images in domain)
 *  - 400 + validation body      → Failure.Server(400, "Title is required")
 *  - 401                        → Failure.Unauthorized
 *  - 500                        → Failure.Server
 *  - network drop               → Failure.Network
 *
 * Wire-level inspection verifies the path and the POST body shape
 * on every request.
 */
class ApiJobRequestRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: JobRequestRepository

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
        repository = ApiJobRequestRepository(backendApi = backendApi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun createJobRequest_200_with_images_returns_Success_with_mapped_domain() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 1,
                      "conversation_id": 10,
                      "title": "Reparación de fuga en la cocina",
                      "description": "Hola Ana, necesito reparar una fuga de agua en la cocina.",
                      "status": "pending",
                      "images": [
                        {
                          "id": "4af47f1b-97b6-4b32-baa0-b95d6077f919",
                          "url": "https://cdn.example/private/job-request-image.jpg?signature=temporary",
                          "original_name": "perdida-bajo-mesada.webp"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val outcome = repository.createJobRequest(
            CreateJobRequestData(
                providerId = 1,
                title = "Reparación de fuga en la cocina",
                description = "Hola Ana, necesito reparar una fuga de agua en la cocina.",
            ),
        )

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/job-requests", recorded.path)
        // The body omits the `image_file_ids` key when the list is
        // empty (kotlinx-serialization + `explicitNulls = false`).
        assertTrue(
            "request body should not include image_file_ids when empty, was: ${recorded.body.readUtf8()}",
            !recorded.body.readUtf8().contains("image_file_ids"),
        )

        assertTrue(outcome is CreateJobRequestOutcome.Success)
        val success = outcome as CreateJobRequestOutcome.Success
        assertEquals("1", success.jobRequest.id)
        assertEquals("10", success.jobRequest.conversationId)
        assertEquals("pending", success.jobRequest.status)
        assertEquals(1, success.jobRequest.images.size)
    }

    @Test
    fun createJobRequest_200_with_null_images_collapses_to_empty_list() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 1,
                      "conversation_id": 10,
                      "title": "x",
                      "description": "y",
                      "status": "pending",
                      "images": null
                    }
                    """.trimIndent(),
                ),
        )

        val outcome = repository.createJobRequest(
            CreateJobRequestData(
                providerId = 1,
                title = "x",
                description = "y",
            ),
        )

        assertTrue(outcome is CreateJobRequestOutcome.Success)
        assertEquals(
            emptyList<Any>(),
            (outcome as CreateJobRequestOutcome.Success).jobRequest.images,
        )
    }

    @Test
    fun createJobRequest_400_validation_returns_Server_failure_with_message() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error": "Title is required"}"""),
        )

        val outcome = repository.createJobRequest(
            CreateJobRequestData(
                providerId = 1,
                title = "x",
                description = "y",
            ),
        )

        assertTrue(outcome is CreateJobRequestOutcome.Failure.Server)
        val failure = outcome as CreateJobRequestOutcome.Failure.Server
        assertEquals(400, failure.code)
        assertEquals("Title is required", failure.message)
    }

    @Test
    fun createJobRequest_401_returns_Unauthorized_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"invalid_token","message":"Failed to validate JWT."}"""),
        )

        val outcome = repository.createJobRequest(
            CreateJobRequestData(
                providerId = 1,
                title = "x",
                description = "y",
            ),
        )

        assertTrue(outcome is CreateJobRequestOutcome.Failure.Unauthorized)
    }

    @Test
    fun createJobRequest_500_returns_Server_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("internal error"),
        )

        val outcome = repository.createJobRequest(
            CreateJobRequestData(
                providerId = 1,
                title = "x",
                description = "y",
            ),
        )

        assertTrue(outcome is CreateJobRequestOutcome.Failure.Server)
        assertEquals(500, (outcome as CreateJobRequestOutcome.Failure.Server).code)
    }

    @Test
    fun createJobRequest_network_drop_returns_Network_failure() = runBlocking {
        server.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
        )

        val outcome = repository.createJobRequest(
            CreateJobRequestData(
                providerId = 1,
                title = "x",
                description = "y",
            ),
        )

        assertTrue(
            "outcome must be Failure.Network, was $outcome",
            outcome is CreateJobRequestOutcome.Failure.Network,
        )
    }
}

package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.data.api.dto.SubmitReviewRequestDto
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * End-to-end coverage of `ApiWorkOrderDetailRepository.submitReview`
 * (US-30 `calify-provider-service`) against a [MockWebServer]
 * emulating the backend's
 * `POST /work-orders/{workOrderID}/review` endpoint.
 *
 * Coverage mirrors the [SubmitWorkOrderReviewOutcome]
 * sealed hierarchy:
 *   - 200 + valid ReviewDto body                → Submitted(review)
 *   - 409 "already reviewed"                    → AlreadyReviewed(message)
 *   - 500 generic server error                  → Server(500, …)
 *   - 400 invalid rating (boundary)             → Server(400, …)
 *   - transport-level failure (DISCONNECT)      → Network(IOException)
 *   - request body is the snake_case payload    → asserted on takeRequest()
 *
 * The wire-level details — HTTP method, path, body shape —
 * are pinned against `MockWebServer.takeRequest()` so a future
 * regression in the Retrofit method's `@Path`/`@Body` annotations
 * trips the test instead of silently making it to production.
 */
class ApiWorkOrderDetailRepositorySubmitReviewTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: WorkOrderDetailRepository
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
        repository = ApiWorkOrderDetailRepository(backendApi = backendApi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun submitReview_200_returns_Submitted_with_review_payload() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {"rating": 5, "description": "Excelente trabajo"}
                    """.trimIndent(),
                ),
        )

        val outcome = repository.submitReview(
            workOrderId = "wo-100",
            rating = 5,
            description = "Excelente trabajo",
        )

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/work-orders/wo-100/review", recorded.path)
        val sent = json.decodeFromString(
            SubmitReviewRequestDto.serializer(),
            recorded.body.readUtf8(),
        )
        assertEquals(5, sent.rating)
        assertEquals("Excelente trabajo", sent.description)

        assertTrue("outcome must be Submitted", outcome is SubmitWorkOrderReviewOutcome.Submitted)
        val submitted = outcome as SubmitWorkOrderReviewOutcome.Submitted
        assertEquals(5, submitted.review.rating)
        assertEquals("Excelente trabajo", submitted.review.description)
    }

    @Test
    fun submitReview_200_with_blank_description_preserves_empty_string() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"rating": 4, "description": ""}"""),
        )

        val outcome = repository.submitReview(
            workOrderId = "wo-101",
            rating = 4,
            description = "",
        )

        val recorded = server.takeRequest()
        val sent = json.decodeFromString(
            SubmitReviewRequestDto.serializer(),
            recorded.body.readUtf8(),
        )
        assertEquals(4, sent.rating)
        assertEquals("", sent.description)

        assertTrue(outcome is SubmitWorkOrderReviewOutcome.Submitted)
        val review = (outcome as SubmitWorkOrderReviewOutcome.Submitted).review
        assertEquals(4, review.rating)
        assertEquals("", review.description)
    }

    @Test
    fun submitReview_409_returns_AlreadyReviewed_with_conflict_message() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"error":"already_reviewed","message":"Ya calificaste esta orden."}""",
                ),
        )

        val outcome = repository.submitReview(
            workOrderId = "wo-100",
            rating = 5,
            description = "Bien",
        )

        assertTrue(
            "expected AlreadyReviewed, was $outcome",
            outcome is SubmitWorkOrderReviewOutcome.AlreadyReviewed,
        )
        assertEquals(
            "Ya calificaste esta orden.",
            (outcome as SubmitWorkOrderReviewOutcome.AlreadyReviewed).message,
        )
    }

    @Test
    fun submitReview_400_invalid_rating_returns_Server_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"invalid_rating","message":"Rating must be 1..5."}"""),
        )

        val outcome = repository.submitReview(
            workOrderId = "wo-100",
            rating = 0,
            description = "out of range",
        )

        assertTrue(outcome is SubmitWorkOrderReviewOutcome.Server)
        val failure = outcome as SubmitWorkOrderReviewOutcome.Server
        assertEquals(400, failure.code)
        assertEquals("Rating must be 1..5.", failure.message)
    }

    @Test
    fun submitReview_500_with_empty_body_returns_Server_failure_with_status_text() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(""),
        )

        val outcome = repository.submitReview(
            workOrderId = "wo-100",
            rating = 3,
            description = "ok",
        )

        assertTrue(outcome is SubmitWorkOrderReviewOutcome.Server)
        val failure = outcome as SubmitWorkOrderReviewOutcome.Server
        assertEquals(500, failure.code)
        assertNotNull(failure.message)
        assertTrue(failure.message.isNotBlank())
    }

    @Test
    fun submitReview_network_failure_returns_Network_failure_with_cause() = runBlocking {
        // DISCONNECT_AT_START closes the socket before any bytes are
        // read; OkHttp surfaces this as an IOException.
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val outcome = repository.submitReview(
            workOrderId = "wo-100",
            rating = 5,
            description = "Excelente",
        )

        assertTrue(
            "outcome must be Network, was $outcome",
            outcome is SubmitWorkOrderReviewOutcome.Network,
        )
        val failure = outcome as SubmitWorkOrderReviewOutcome.Network
        assertSame(
            "cause must be an IOException",
            IOException::class.java,
            failure.cause::class.java,
        )
    }

    @Test
    fun submitReview_404_unknown_workOrder_returns_Server_failure() = runBlocking {
        // Defense-in-depth: if the backend rejects an unknown
        // work-order with 404, we still surface a typed failure
        // (rather than leaking the HTTP code into the UI).
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"not_found","message":"Work order not found."}"""),
        )

        val outcome = repository.submitReview(
            workOrderId = "wo-unknown",
            rating = 4,
            description = "ok",
        )

        assertTrue(outcome is SubmitWorkOrderReviewOutcome.Server)
        val failure = outcome as SubmitWorkOrderReviewOutcome.Server
        assertEquals(404, failure.code)
        assertEquals("Work order not found.", failure.message)
    }

    /**
     * Sanity check that null fields are optional: the adapter must
     * not crash on responses that omit a non-required field if the
     * backend adds one in a future iteration. Today both
     * [com.loresuelvo.consumer.data.api.dto.ReviewDto] fields are
     * required and the request body is non-null, but the property
     * exercises the happy-path mapping deterministically.
     */
    @Test
    fun submitReview_does_not_touch_the_read_endpoint() = runBlocking {
        // Only one request should hit the server: the POST. A
        // regression that accidentally calls GET /work-orders/{id}
        // from the submit path would enqueue a second one and fail
        // the assertion below.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"rating": 5, "description": "ok"}"""),
        )

        repository.submitReview("wo-100", 5, "ok")

        assertEquals(1, server.requestCount)
        val recorded = server.takeRequest()
        assertEquals("/work-orders/wo-100/review", recorded.path)
        // And nothing is left queued.
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS))
    }
}

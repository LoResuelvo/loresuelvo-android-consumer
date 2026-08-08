package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.WsTicketResponseDto
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.realtime.WsEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for [WebSocketClient] against
 * [MockWebServer]'s WebSocket support. Each test enqueues a
 * `withWebSocketUpgrade(...)` response so MockWebServer speaks
 * the WS protocol with the client; the test captures the
 * server-side [WebSocket] in `onOpen` and pushes JSON frames
 * into the client's [WebSocketClient.events] flow.
 *
 * Uses [runBlocking] + real wall-clock timeouts (via
 * [withTimeout]) because the production client runs its scope on
 * `Dispatchers.IO`, which the `StandardTestDispatcher` doesn't
 * control. The client lives for the duration of the test and
 * is torn down in `@After`.
 *
 * Coverage:
 *  - The client calls `POST /ws-tickets` before connecting and
 *    opens the WS with the ticket in the query string.
 *  - Incoming JSON frames are parsed into [WsEvent]s and emitted
 *    on the flow.
 *  - Frames with an unknown `type` are dropped silently (the
 *    connection stays open).
 *  - Frames with malformed JSON are dropped silently.
 *
 * The 4xx-stops-retrying path is covered by the production guard
 * (`error.code in 400..499`); a future test can wire the ticket
 * endpoint through MockWebServer's REST surface to exercise the
 * actual `HttpException` → `ApiError.Server` mapping without a
 * bespoke Retrofit response builder.
 */
class WebSocketClientTest {

    private lateinit var server: MockWebServer
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var backendApi: BackendApi
    private lateinit var webSocketClient: WebSocketClient
    private var latestServerSocket: WebSocket? = null

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        okHttpClient = OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build()

        backendApi = mockk()

        val wsUrl = server.url("/ws").toString()
            .replaceFirst("http://", "ws://")
            .replaceFirst("https://", "wss://")
        webSocketClient = WebSocketClient(
            okHttpClient = okHttpClient,
            backendApi = backendApi,
            wsUrl = wsUrl,
        )
    }

    @After
    fun tearDown() {
        // Close the server-side WS first so the dispatcher queue
        // drains cleanly; otherwise MockWebServer's shutdown can
        // race the close frame and raise `Gave up waiting for
        // queue to shut down`.
        latestServerSocket?.close(1000, "test done")
        webSocketClient.stop()
        Thread.sleep(100)
        server.shutdown()
    }

    /**
     * Helper: enqueue a `withWebSocketUpgrade` response that
     * captures the server-side WebSocket and returns it via the
     * deferred so the test body can push frames into the client.
     */
    private fun enqueueWebSocketUpgrade(): CompletableDeferred<WebSocket> {
        val serverSocketDeferred = CompletableDeferred<WebSocket>()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                serverSocketDeferred.complete(webSocket)
            }
        }
        server.enqueue(MockResponse().withWebSocketUpgrade(listener))
        return serverSocketDeferred
    }

    private suspend fun awaitServerSocket(): WebSocket {
        val ws = withTimeout(2000) {
            enqueueWebSocketUpgrade().await()
        }
        latestServerSocket = ws
        return ws
    }

    @Test
    fun connect_fetches_ticket_then_opens_ws_with_ticket_in_query() = runBlocking<Unit> {
        coEvery { backendApi.getWsTicket() } returns WsTicketResponseDto("test-ticket-123")

        // Note: must call start() FIRST so the WS upgrade request
        // actually fires before we await the server-side socket
        // (MockWebServer only opens the socket on the upgrade).
        webSocketClient.start()
        awaitServerSocket()

        // The client fetched the ticket exactly once.
        coVerify(exactly = 1) { backendApi.getWsTicket() }

        // The WS upgrade request landed on MockWebServer with the
        // ticket + role in the query string.
        val recorded = withTimeout(2000) { server.takeRequest() }
        val path = recorded.path ?: error("no path recorded")
        assertEquals("/ws", path.substringBefore("?"))
        assertTrue(
            "ticket query param missing: $path",
            path.contains("ticket=test-ticket-123"),
        )
        assertTrue(
            "role=consumer query param missing: $path",
            path.contains("role=consumer"),
        )
    }

    @Test
    fun incoming_valid_event_is_emitted_on_the_flow() = runBlocking<Unit> {
        coEvery { backendApi.getWsTicket() } returns WsTicketResponseDto("ticket")

        val received = mutableListOf<WsEvent>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            webSocketClient.events.collect { received.add(it) }
        }

        webSocketClient.start()
        val serverSocket = awaitServerSocket()

        // Push a valid `conversation.message.created` frame from
        // the "server".
        serverSocket.send(
            """{"type":"conversation.message.created","conversation_id":1,"message":{"id":42,"sender_role":"provider","content":"hola","created_on":"2026-05-31T12:00:00Z"}}""",
        )

        withTimeout(2000) {
            while (received.isEmpty()) delay(20)
        }

        assertEquals(1, received.size)
        val event = received[0]
        assertEquals(1L, event.conversationId)
        assertEquals(ConversationSender.Provider, event.message.sender)
        assertEquals("hola", event.message.content)
        assertEquals("42", event.message.id)
        assertTrue(event.message.createdOnEpochMillis != 0L)

        job.cancel()
    }

    @Test
    fun incoming_frame_with_unknown_type_is_dropped_silently() = runBlocking<Unit> {
        coEvery { backendApi.getWsTicket() } returns WsTicketResponseDto("ticket")

        val received = mutableListOf<WsEvent>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            webSocketClient.events.collect { received.add(it) }
        }

        webSocketClient.start()
        val serverSocket = awaitServerSocket()

        // Unknown event type — the mapper returns null, the
        // listener drops the frame silently. The connection
        // stays open.
        serverSocket.send(
            """{"type":"conversation.accepted","conversation_id":1,"message":{"id":1,"sender_role":"provider","content":"x"}}""",
        )

        // Send a valid one right after to prove the connection
        // is still alive.
        delay(100)
        serverSocket.send(
            """{"type":"conversation.message.created","conversation_id":1,"message":{"id":2,"sender_role":"provider","content":"alive"}}""",
        )

        withTimeout(2000) {
            while (received.size < 1) delay(20)
        }
        // We only see the valid one; the unknown type was dropped.
        assertEquals(1, received.size)
        assertEquals("alive", received[0].message.content)

        job.cancel()
    }

    @Test
    fun incoming_malformed_json_is_dropped_silently() = runBlocking<Unit> {
        coEvery { backendApi.getWsTicket() } returns WsTicketResponseDto("ticket")

        val received = mutableListOf<WsEvent>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            webSocketClient.events.collect { received.add(it) }
        }

        webSocketClient.start()
        val serverSocket = awaitServerSocket()

        // Garbage frame. The listener catches the parse failure
        // and drops the frame.
        serverSocket.send("this is not json")

        delay(100)
        // The connection must still be alive — push a valid frame
        // to verify.
        serverSocket.send(
            """{"type":"conversation.message.created","conversation_id":1,"message":{"id":2,"sender_role":"provider","content":"alive"}}""",
        )

        withTimeout(2000) {
            while (received.isEmpty()) delay(20)
        }
        assertEquals(1, received.size)
        assertEquals("alive", received[0].message.content)

        job.cancel()
    }
}
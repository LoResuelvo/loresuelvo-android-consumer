package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.WsEventDto
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.realtime.WsEvent
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * App-wide WebSocket client for the conversation real-time
 * updates (scenarios 07-10-IC). Opens a single connection against
 * the backend's `/ws` endpoint, authenticates with the
 * short-lived ticket fetched from `POST /ws-tickets`, and emits
 * decoded [WsEvent]s as a [SharedFlow] for any ViewModel that
 * subscribes (typically [com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel]
 * for scenario 07-IC).
 *
 * Lifecycle:
 *  - [start] launches a coroutine that fetches a ticket and
 *    opens the connection.
 *  - On `onClosed` / `onFailure` the client schedules a
 *    reconnect with a 3s delay (matches the webapp's
 *    `WebSocketProvider`). A 4xx ticket-fetch failure stops
 *    retrying (the auth is broken); a 5xx or transport failure
 *    retries indefinitely.
 *  - [stop] closes the connection and cancels the supervisor
 *    scope. Re-calling [start] after [stop] is a no-op (the
 *    client is one-shot per app session).
 *
 * The `events` flow uses `DROP_OLDEST` overflow because the
 * ViewModel-side filter (by `conversationId`) drops everything
 * we don't care about — losing a stale event is preferable to
 * blocking the WebSocket dispatcher thread.
 */
@Singleton
class WebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val backendApi: BackendApi,
    @Named("wsUrl") private val wsUrl: String,
) {
    private val _events = MutableSharedFlow<WsEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }
    private val reconnectDelays = ReconnectDelays()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var reconnectJob: Job? = null

    @Volatile
    private var stopped = false

    fun start() {
        // Idempotent: callers (currently the conversation
        // `ConversationViewModel.init {}`) invoke this on every
        // screen entry. After the first call the connection stays
        // up across the app session; subsequent calls are no-ops.
        if (stopped) return
        if (webSocket != null || reconnectJob?.isActive == true) return
        scope.launch { connect() }
    }

    fun stop() {
        stopped = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "client stopping")
        scope.cancel()
    }

    private suspend fun connect() {
        if (stopped) return
        val ticket = try {
            backendApi.getWsTicket().ticket
        } catch (t: Throwable) {
            val error = t.toApiError()
            // 4xx is a hard failure (auth broken, role revoked,
            // etc.) — never retry. 5xx and transport failures
            // retry with a slightly longer delay.
            if (error is ApiError.Server && error.code in 400..499) {
                return
            }
            scheduleReconnect(reconnectDelays.nextTransportDelay())
            return
        }
        val request = Request.Builder()
            .url("$wsUrl?ticket=$ticket&role=consumer")
            .build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    private val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            val event = parseFrame(text) ?: return
            // emit via the scope — SharedFlow.emit suspends only
            // when the buffer is full; with DROP_OLDEST it never
            // suspends so this is non-blocking.
            scope.launch { _events.emit(event) }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            this@WebSocketClient.webSocket = null
            if (!stopped) scheduleReconnect(reconnectDelays.nextDisconnectDelay())
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            this@WebSocketClient.webSocket = null
            if (!stopped) scheduleReconnect(reconnectDelays.nextTransportDelay())
        }
    }

    private fun parseFrame(text: String): WsEvent? = try {
        json.decodeFromString<WsEventDto>(text).toDomain()
    } catch (_: Throwable) {
        // Drop unparseable frames silently — the backend may
        // ship a new event type we don't know yet, and a
        // hard crash on a single bad frame would tear down the
        // connection.
        null
    }

    private fun scheduleReconnect(delay: kotlin.time.Duration) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delay)
            if (!isActive) return@launch
            connect()
        }
    }

    /**
     * Backoff schedule. Disconnects (clean close) retry at 3s
     * flat; transport failures start at 5s and grow up to 30s so
     * the client doesn't hammer the backend when the dev server
     * is down.
     */
    private class ReconnectDelays {
        private var transportAttempts = 0

        fun nextDisconnectDelay(): kotlin.time.Duration = 3.seconds

        fun nextTransportDelay(): kotlin.time.Duration {
            transportAttempts += 1
            // 5s, 10s, 20s, 30s, 30s, ...
            val seconds = (5L shl (transportAttempts - 1).coerceAtMost(2)).coerceAtMost(30L)
            return seconds.seconds
        }
    }

    private companion object {
        // 0 is a sentinel for "no reconnect scheduled yet"; the
        // millis accessor is exposed for tests that want to
        // assert the schedule.
        val DEFAULT_RECONNECT_DELAY_MS: Long = 3000L.milliseconds.inWholeMilliseconds
    }
}
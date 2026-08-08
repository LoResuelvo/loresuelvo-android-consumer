package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for `POST /ws-tickets`. The backend returns a
 * short-lived signed JWT (the "ticket") that the Android client
 * passes as a query parameter when opening the WebSocket:
 *
 * ```
 * wss://host/ws?ticket=<ticket>&role=<consumer|provider>
 * ```
 *
 * Mirrors the webapp's `POST /api/ws-tickets` response shape
 * (`{ ticket: string }`). The ticket is intentionally separate
 * from the long-lived Auth0 access token so the WebSocket
 * connection can be torn down independently (the ticket has a
 * short TTL) and so the upgrade URL can be inspected by load
 * balancers without leaking the access token.
 */
@Serializable
data class WsTicketResponseDto(
    @SerialName("ticket") val ticket: String,
)
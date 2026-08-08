package com.loresuelvo.consumer.domain.realtime

import com.loresuelvo.consumer.domain.conversation.ConversationMessage

/**
 * One real-time push event from the backend's WebSocket. Mirrors
 * the wire shape documented in
 * `infrastructure/websocket/types.ts` of the webapp and the
 * scenario wire in `send-messages.feature`:
 *
 * ```
 * {
 *   "type": "conversation.message.created",
 *   "conversation_id": 1,
 *   "message": { "id": 2, "sender_role": "consumer", ... }
 * }
 * ```
 *
 * The Android consumer app subscribes per-conversation (the
 * WebSocket is shared app-wide; each `ConversationViewModel`
 * filters events for its current `conversationId` and drops the
 * rest — see scenario 08-IC).
 *
 * Today the only `type` the backend emits is
 * [CONVERSATION_MESSAGE_CREATED]. When a second event type
 * lands, [type] should become a sealed hierarchy so the VM
 * (and any future tests) can `when`-exhaust on it.
 *
 * Pure domain: reuses [ConversationMessage] (same shape as the
 * REST `GET /conversations/{id}` `messages[]` entries, mapped
 * through the same mapper). No wire types live here.
 */
data class WsEvent(
    val type: String,
    val conversationId: Long,
    val message: ConversationMessage,
) {
    companion object {
        /**
         * WebSocket discriminator the backend emits when a new
         * message is appended to a conversation. The constant is
         * here (not in the data layer) so the mapper and any
         * future event consumer can compare against a stable
         * name without re-importing the wire constant.
         */
        const val CONVERSATION_MESSAGE_CREATED: String =
            "conversation.message.created"
    }
}
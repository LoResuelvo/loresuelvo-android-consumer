package com.loresuelvo.consumer.domain.diagnosis

/**
 * Image attached to an AI diagnostic chat message.
 *
 * While sending, the URL can reference the selected local file. A
 * successful response replaces it with the server's private, short-lived
 * URL. These references are never persisted by the app.
 */
data class ChatImage(
    val id: String,
    val url: String,
    val originalName: String,
    val mimeType: String,
)

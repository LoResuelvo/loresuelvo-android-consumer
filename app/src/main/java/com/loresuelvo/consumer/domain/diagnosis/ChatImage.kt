package com.loresuelvo.consumer.domain.diagnosis

/**
 * Server-issued image attached to an AI diagnostic chat message.
 *
 * The URL may be private and short-lived; it is consumed directly by
 * the presentation layer and is never persisted by the app.
 */
data class ChatImage(
    val id: String,
    val url: String,
    val originalName: String,
    val mimeType: String,
)

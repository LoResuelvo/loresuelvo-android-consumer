package com.loresuelvo.consumer.domain.jobrequest

/**
 * Image attached to a [JobRequest]. Pure domain type: camelCase,
 * no framework deps. The wire format is mapped in
 * `data/api/mapper/JobRequestDtoMapper.kt`.
 *
 * `url` is a presigned URL scoped to the conversation — it is
 * fetched fresh when the consumer or provider opens the chat.
 * The application does not need to (and should not) cache it.
 */
data class JobRequestImage(
    val id: String,
    val url: String,
    val originalName: String,
)

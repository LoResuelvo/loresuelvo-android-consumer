package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.CreateJobRequestDto
import com.loresuelvo.consumer.data.api.dto.JobRequestDto
import com.loresuelvo.consumer.data.api.dto.JobRequestImageDto
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import com.loresuelvo.consumer.domain.jobrequest.JobRequest
import com.loresuelvo.consumer.domain.jobrequest.JobRequestImage

/**
 * DTO ↔ domain translation for the `POST /job-requests` endpoint.
 *
 * Boundary rules:
 *  - `Long` ids on the wire → `String` in the domain (stable
 *    LazyColumn keys, no overflow).
 *  - `conversation_id` is nullable on the wire (the backend may
 *    omit it when the conversation hasn't been materialised yet)
 *    and stays nullable in the domain — the UI handles the
 *    navigation event regardless.
 *  - `images` is `null` on the wire when the request carried no
 *    attachments; the mapper collapses it to `emptyList()` so the
 *    domain type can stay non-nullable.
 *  - `imageFileIds` defaults to `emptyList()` in the domain; the
 *    mapper drops an empty list from the JSON payload (the
 *    backend treats the field as optional).
 */
internal fun JobRequestDto.toDomain(): JobRequest = JobRequest(
    id = id.toString(),
    conversationId = conversationId?.toString(),
    title = title,
    description = description,
    status = status,
    images = images?.map { it.toDomain() } ?: emptyList(),
)

internal fun JobRequestImageDto.toDomain(): JobRequestImage = JobRequestImage(
    id = id,
    url = url,
    originalName = originalName,
)

internal fun CreateJobRequestData.toDto(): CreateJobRequestDto = CreateJobRequestDto(
    providerId = providerId,
    title = title,
    description = description,
    // Drop the key from the JSON payload when the consumer did not
    // attach any images (the modal form does not expose upload
    // today). The backend accepts either null or an array of ids.
    imageFileIds = imageFileIds.takeIf { it.isNotEmpty() },
)

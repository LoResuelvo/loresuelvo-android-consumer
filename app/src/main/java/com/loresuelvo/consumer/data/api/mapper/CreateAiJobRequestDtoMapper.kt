package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.CreateAiJobRequestRequestDto

/**
 * Domain → DTO translation for the AI pre-filled job-request
 * payload. Single-direction helper: the response wire shape is
 * identical to `JobRequestDto` (both endpoints return
 * `{id, conversation_id, title, description, status, images[]}`)
 * so the existing `JobRequestDtoMapper.toDomain()` handles the
 * response direction without a new mapper.
 */
internal fun createAiJobRequestRequest(providerId: Int): CreateAiJobRequestRequestDto =
    CreateAiJobRequestRequestDto(providerId = providerId)

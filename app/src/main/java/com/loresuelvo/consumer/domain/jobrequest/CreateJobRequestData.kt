package com.loresuelvo.consumer.domain.jobrequest

/**
 * Input payload for [JobRequestRepository.createJobRequest]. The
 * fields are mapped to the backend's `POST /job-requests` wire
 * format in `data/api/dto/CreateJobRequestDto` + the mapper at
 * `data/api/mapper/JobRequestDtoMapper.kt`.
 *
 * `imageFileIds` defaults to empty (the modal form does not
 * expose image upload today). The backend treats the field as
 * optional, so the empty list is omitted from the JSON payload.
 */
data class CreateJobRequestData(
    val providerId: Int,
    val title: String,
    val description: String,
    val imageFileIds: List<String> = emptyList(),
)

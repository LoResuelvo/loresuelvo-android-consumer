package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequestDto(
    @SerialName("content") val content: String,
    @SerialName("image_file_ids") val imageFileIds: List<String>? = null,
    @SerialName("audio_file_id") val audioFileId: String? = null,
    @SerialName("video_file_id") val videoFileId: String? = null,
)

package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationMessageDto(
    @SerialName("id") val id: Long,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("content") val content: String,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("images") val images: List<MessageImageDto> = emptyList(),
    @SerialName("audio") val audio: MessageAudioDto? = null,
    @SerialName("video") val video: MessageVideoDto? = null,
)

package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    @SerialName("id") val id: Long,
    @SerialName("status") val status: String,
    @SerialName("counterpart") val counterpart: ConversationCounterpartDto,
    @SerialName("last_message") val lastMessage: ConversationMessageDto? = null,
    @SerialName("updated_on") val updatedOn: String? = null,
)
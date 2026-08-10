package com.loresuelvo.consumer.domain.diagnosis

import com.loresuelvo.consumer.domain.provider.Provider

data class Diagnosis(
    val conversationId: String?,
    val messages: List<ChatMessage>,
    val assessment: String? = null,
    val recommendedProviders: List<Provider>? = null,
)

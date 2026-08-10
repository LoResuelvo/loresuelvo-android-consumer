package com.loresuelvo.consumer.domain.diagnosis

import com.loresuelvo.consumer.domain.provider.Provider

/**
 * Aggregate returned by the AI diagnosis backend after the consumer
 * sends a message. Carries:
 *
 *  - [conversationId]: the backend-issued conversation id.
 *  - [messages]: full ordered history (consumer → assistant).
 *  - [assessment]: optional block attached when the AI concludes.
 *    Lands on the LAST assistant message at the UI layer.
 *  - [recommendedProviders]: optional list the AI suggests once
 *    it has matched a category; rendered by the chat surface when
 *    the assessment is non-null.
 */
data class Diagnosis(
    val conversationId: String?,
    val messages: List<ChatMessage>,
    val assessment: DiagnosisAssessment? = null,
    val recommendedProviders: List<Provider>? = null,
)

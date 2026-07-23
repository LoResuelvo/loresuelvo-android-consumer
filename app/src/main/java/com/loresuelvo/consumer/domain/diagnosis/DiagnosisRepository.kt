package com.loresuelvo.consumer.domain.diagnosis

/**
 * Port for the AI diagnostic chat backend. Implementations live in
 * `data/api/` and translate the wire contract (snake_case JSON over
 * the Retrofit-typed [com.loresuelvo.consumer.data.api.BackendApi])
 * into the domain's [SendDiagnosisPromptOutcome] hierarchy.
 *
 * The repository never throws on HTTP / network failures: every
 * exception is mapped to a typed [SendDiagnosisPromptOutcome.Failure]
 * (exhaustive `when` against [com.loresuelvo.consumer.domain.api.ApiError]).
 * Implementations must remain pure with respect to UI concerns —
 * no Android, no kotlinx-serialization, no Hilt.
 */
interface DiagnosisRepository {

    /**
     * Sends the consumer's prompt to the AI diagnosis backend.
     *
     *  - [existingConversationId] `null` ⇒ create a new
     *    conversation (`POST /chatbot/conversations`).
     *  - [existingConversationId] non-null ⇒ append to the
     *    conversation (`POST /chatbot/conversations/{id}/messages`).
     *
     * Returns the full conversation aggregate ([Diagnosis]) on
     * success, or a typed failure otherwise.
     */
    suspend fun sendPrompt(
        content: String,
        existingConversationId: String? = null,
    ): SendDiagnosisPromptOutcome
}

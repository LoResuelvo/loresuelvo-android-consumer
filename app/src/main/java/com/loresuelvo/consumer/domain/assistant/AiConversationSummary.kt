package com.loresuelvo.consumer.domain.assistant

/**
 * Thin summary of an AI diagnostic conversation for the
 * "Asistente IA" tab list. Distinct from the full [Diagnosis]
 * aggregate (which carries the messages + assessment + providers
 * for the chat thread): this is the row-level payload the
 * list surface renders to the consumer.
 *
 *  - [id]: backend-issued conversation id (stringified on the
 *    domain side so `LazyColumn` keys stay stable across the
 *    long > 2^63 range).
 *  - [title]: human-readable summary the AI wrote on the
 *    conversation (falls back to the first message in the
 *    legacy / no-summary case — the title comes from the
 *    backend's first-message classification).
 *  - [lastMessageAtEpochMillis]: epoch millis of the last
 *    activity on the conversation (when the assistant acknowledged
 *    the last user prompt). Surfaced as "Last activity" on the row.
 *  - [lastMessagePreview]: optional preview of the last message
 *    — useful when the AI's title is a fixed category ("Plomería")
 *    and the user wants to see what they actually wrote last.
 *
 * `status` / `responseStatus` / `diagnosisCompleted` from the
 * wire are not surfaced here yet; a future ticket can promote
 * them to surface-level "Continuar chat" vs "Ver resumen"
 * affordances.
 */
data class AiConversationSummary(
    val id: String,
    val title: String,
    val lastMessageAtEpochMillis: Long,
    val lastMessagePreview: String? = null,
)

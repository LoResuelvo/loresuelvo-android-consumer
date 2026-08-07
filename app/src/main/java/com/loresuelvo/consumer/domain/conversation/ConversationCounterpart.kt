package com.loresuelvo.consumer.domain.conversation

/**
 * The other party in a [Conversation] as the consumer sees it.
 * Today this is always the provider, but the field is kept neutral
 * ("counterpart") so future versions of the app (provider app,
 * multi-party chat) can reuse the type without renaming.
 *
 *  - [id] is the provider's backend id (numeric in the wire).
 *  - [name] / [surname] are split so the UI can render either form
 *    (avatar initial from [name], full label from "$name $surname").
 *  - [categoryName] is denormalized in the response so the list
 *    cell can render "Plomería" under the provider's name without
 *    a second round-trip.
 *  - [profilePhotoUrl] is `null` when the provider has not uploaded
 *    one; the UI must fall back to the initial-letter avatar (see
 *    `ProviderAvatar` in `ui/screens/professional/`).
 *
 * Pure domain.
 */
data class ConversationCounterpart(
    val id: Long,
    val name: String,
    val surname: String,
    val categoryName: String,
    val profilePhotoUrl: String?,
)
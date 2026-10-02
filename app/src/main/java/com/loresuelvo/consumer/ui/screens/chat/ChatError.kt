package com.loresuelvo.consumer.ui.screens.chat

import androidx.annotation.StringRes
import com.loresuelvo.consumer.R

/**
 * UI-facing error state for the AI diagnostic chat screen.
 *
 * `ServiceUnavailable` covers all transport / backend errors that
 * aren't credential-related — DNS, network drop, 5xx, timeouts.
 * The user-visible message reads "No pudimos obtener una respuesta
 * en este momento" and pairs with a retry CTA so the consumer can
 * resubmit the last prompt without retyping it.
 *
 * `Network` represents a transport failure and has its own localized
 * message. `Unauthorized` preserves the provider message so the
 * session layer can decide how to recover.
 *
 * The mapping to a `@StringRes` is `messageResId()` (used by the
 * Composable); `errorLiteral()` exposes the stable Spanish copy
 * used by the chat's non-Compose consumers.
 */
sealed interface ChatError {
    data object ServiceUnavailable : ChatError
    data object Network : ChatError
    data class Unauthorized(val message: String) : ChatError
}

@StringRes
fun ChatError.messageResId(): Int = when (this) {
    ChatError.ServiceUnavailable -> R.string.chat_error_service_unavailable
    ChatError.Network -> R.string.chat_error_network
    is ChatError.Unauthorized -> R.string.chat_error_unauthorized
}

/**
 * Stable Spanish literal exposed without requiring the Android
 * resource graph. It must stay in sync with the resource value at
 * `app/src/main/res/values/strings.xml#chat_error_service_unavailable`.
 */
fun ChatError.errorLiteral(): String = when (this) {
    ChatError.ServiceUnavailable -> "No pudimos obtener una respuesta en este momento"
    ChatError.Network -> "No pudimos conectarnos al servicio. Revisá tu conexión."
    is ChatError.Unauthorized -> message
}

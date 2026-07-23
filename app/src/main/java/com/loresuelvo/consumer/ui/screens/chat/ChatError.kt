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
 * `Network` is reserved for a future stricter copy ("sin
 * conexión"); current 04-DIA groups it under ServiceUnavailable.
 * `Unauthorized` is reserved for Auth0 token expiry flows and is
 * not exercised by 04-DIA — added now to keep the surface area
 * future-proof.
 *
 * The mapping to a `@StringRes` is `messageResId()` (used by the
 * Composable); the literal text on the wire (BDD) is
 * `errorLiteral()` which mirrors the value the resource resolves
 * to under the device locale.
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
 * Stable Spanish literal surfaced to the BDD layer so scenario
 * 04-DIA's `Then veo el mensaje del asistente {string}` matches
 * the user-visible text without needing the Android resource
 * graph. Must stay in sync with the resource value at
 * `app/src/main/res/values/strings.xml#chat_error_service_unavailable`.
 */
fun ChatError.errorLiteral(): String = when (this) {
    ChatError.ServiceUnavailable -> "No pudimos obtener una respuesta en este momento"
    ChatError.Network -> "No pudimos conectarnos al servicio. Revisá tu conexión."
    is ChatError.Unauthorized -> message
}

package com.loresuelvo.consumer.ui.navigation

import android.net.Uri

/**
 * Helpers to extract query parameters from an [Uri] captured by
 * Compose Navigation when an `intent-filter` matches the deep link
 * (HTTPS App Link from the Mercado Pago redirect). The graph
 * declares the path in [composable] with
 * `navDeepLink { uriPattern = ... }`; the captured [Uri] is
 * queryable by name.
 */
object PaymentDeepLink {

    /**
     * Returns the `payment_intent_id` query parameter from the
     * given [Uri], or null if it is missing or blank. The backend
     * passes this id via the redirect URL so the consumer can
     * poll `GET /payment-intents/{id}` for the latest status.
     */
    fun paymentIntentId(uri: Uri): String? = uri.getQueryParameter("payment_intent_id")
        ?.takeIf { it.isNotBlank() }

    /**
     * Returns the optional `status` query parameter. MP echoes the
     * `external_reference` as the intent id; the consumer uses
     * `status` only as a hint to short-circuit the first poll.
     */
    fun status(uri: Uri): String? = uri.getQueryParameter("status")
        ?.takeIf { it.isNotBlank() }
}

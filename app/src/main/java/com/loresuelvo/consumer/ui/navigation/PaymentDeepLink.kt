package com.loresuelvo.consumer.ui.navigation

import android.net.Uri

/** Maps supported external payment returns to the internal graph route. */
internal fun paymentResultPathFor(uri: Uri): String? {
    return paymentResultPathFor(
        path = uri.path,
        externalReference = uri.getQueryParameter(Route.ARG_EXTERNAL_REFERENCE),
    )
}

internal fun paymentResultPathFor(
    path: String?,
    externalReference: String?,
): String? {
    val isPaymentReturn = path in setOf(
        Route.PAYMENT_RETURN_SUCCESS_PATH,
        Route.PAYMENT_RETURN_PENDING_PATH,
        Route.PAYMENT_RETURN_FAILURE_PATH,
    )

    return if (isPaymentReturn && !externalReference.isNullOrBlank()) {
        Route.PaymentResult.buildPath(externalReference)
    } else {
        null
    }
}

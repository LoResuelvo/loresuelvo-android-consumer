package com.loresuelvo.consumer.domain.payment

enum class PaymentIntentStatus {
    /**
     * Internal transient state: the intent has been persisted
     * but the checkout session has not been created at Mercado
     * Pago yet. Rare on the client — the API typically emits
     * `checkout_ready` immediately after the intent is saved.
     */
    RequiresCheckout,

    /**
     * The Mercado Pago Checkout Pro session is live. The consumer
     * should open the `checkout_url` in a Custom Tab and let the
     * user complete the payment.
     */
    CheckoutReady,

    /**
     * MP is processing the payment (e.g. pending credit-card
     * authorization). The consumer keeps polling and the UI shows
     * "pago en proceso".
     */
    Processing,

    /**
     * The payment was approved and verified by the backend webhook.
     * The associated service proposal / work order is automatically
     * accepted at this point — no Android-side "accept" call is
     * needed.
     */
    Paid,

    /**
     * The payment was rejected by MP (e.g. declined credit card).
     * The proposal/work order stays in its pre-payment state and the
     * consumer can retry by starting a new checkout session.
     */
    Rejected,
}
package com.loresuelvo.consumer.ui.util

object CurrencyFormatter {

    /**
     * Formats [amountCents] (e.g. `1_500_000L`) as a peso string
     * (e.g. `"$ 15.000"`). Cents below the peso floor are
     * truncated — the UI never surfaces decimals.
     */
    fun formatAmount(amountCents: Long): String {
        val whole = amountCents / 100
        val digits = whole.toString()
        val reversed = digits.reversed()
        val withDots = buildString(reversed.length + reversed.length / 3) {
            reversed.forEachIndexed { index, c ->
                if (index > 0 && index % 3 == 0) append('.')
                append(c)
            }
        }.reversed()
        return "$ $withDots"
    }
}
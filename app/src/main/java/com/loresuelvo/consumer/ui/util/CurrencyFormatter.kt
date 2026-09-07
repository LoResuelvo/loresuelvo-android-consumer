package com.loresuelvo.consumer.ui.util

/**
 * Formats a [Long] amount in cents as a peso string with dot
 * thousands separator and no fractional digits (`$15.000`,
 * `$1.500.000`). Used by:
 *
 *  - `ProposalCard` for the at-a-glance amount column on the
 *    home dashboard's "Mis Servicios" row (US-54).
 *  - `ProposalDetailScreen` for the "Monto acordado" detail row
 *    (US-54 scenario 11-VSP).
 *
 * The formatter is intentionally **not** locale-aware: the brand
 * copy is `$` (no space, no locale-dependent currency code) and
 * the thousands separator is `.` regardless of device locale.
 * Wrapping it in a tiny `object` keeps the rules testable on the
 * JVM (see `CurrencyFormatterTest`) and prevents the screen and
 * the card from drifting apart.
 */
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
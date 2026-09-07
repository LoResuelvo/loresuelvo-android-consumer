package com.loresuelvo.consumer.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure JVM unit tests for [CurrencyFormatter.formatAmount].
 * US-54 scenario 11-VSP pins the "$ 15.000" output shape (peso
 * prefix, dot thousands separator, no decimals) and the BDD step
 * def pins the same observable output through the public
 * formatter — these tests guard the formatter itself against
 * regressions introduced by copy-paste tweaks.
 */
class CurrencyFormatterTest {

    @Test
    fun formats_15000_pesos_as_peso_with_thousands_separator() {
        assertEquals("$ 15.000", CurrencyFormatter.formatAmount(1_500_000L))
    }

    @Test
    fun formats_sub_peso_amounts_as_whole_pesos() {
        // 99 cents is 0 pesos 99 — we floor to whole pesos because
        // the UI never surfaces decimals.
        assertEquals("$ 0", CurrencyFormatter.formatAmount(99L))
    }

    @Test
    fun formats_zero_amount_as_zero_pesos() {
        assertEquals("$ 0", CurrencyFormatter.formatAmount(0L))
    }

    @Test
    fun formats_single_peso_without_thousands_separator() {
        assertEquals("$ 1", CurrencyFormatter.formatAmount(100L))
    }

    @Test
    fun formats_million_pesos_with_two_thousand_groups() {
        assertEquals("$ 1.000.000", CurrencyFormatter.formatAmount(100_000_000L))
    }

    @Test
    fun formats_amounts_with_partial_thousands_groups() {
        // 2_500_000 cents = 25_000 pesos — only one separator.
        assertEquals("$ 25.000", CurrencyFormatter.formatAmount(2_500_000L))
    }
}
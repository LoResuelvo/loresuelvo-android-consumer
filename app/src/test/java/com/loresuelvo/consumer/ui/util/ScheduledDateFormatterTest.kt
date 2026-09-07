package com.loresuelvo.consumer.ui.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure JVM unit tests for [ScheduledDateFormatter.formatScheduled].
 * US-54 scenario 12-VSP pins the `"15/10/2026 - 14:30 hs"` shape;
 * these tests guard the formatter itself against regressions
 * introduced by locale or timezone drift.
 */
class ScheduledDateFormatterTest {

    @Test
    fun formats_october_15_2026_at_14_30_as_pinned() {
        val epoch = parseUtc("15/10/2026 14:30")
        assertEquals(
            "15/10/2026 - 14:30 hs",
            ScheduledDateFormatter.formatScheduled(epoch),
        )
    }

    @Test
    fun zero_pads_single_digit_day_month_and_hour() {
        val epoch = parseUtc("05/01/2026 09:05")
        assertEquals(
            "05/01/2026 - 09:05 hs",
            ScheduledDateFormatter.formatScheduled(epoch),
        )
    }

    @Test
    fun formats_midnight_without_collapsing_to_previous_date() {
        val epoch = parseUtc("01/01/2026 00:00")
        assertEquals(
            "01/01/2026 - 00:00 hs",
            ScheduledDateFormatter.formatScheduled(epoch),
        )
    }

    private fun parseUtc(dateTime: String): Long {
        val parser = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        return parser.parse(dateTime)!!.time
    }
}
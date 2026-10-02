package com.loresuelvo.consumer.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class EstimatedDurationFormatterTest {

    @Test
    fun formats_45_minutes_as_pinned() {
        assertEquals("45 min", EstimatedDurationFormatter.formatDuration(45))
    }

    @Test
    fun formats_60_minutes_as_one_hour() {
        assertEquals("1 h", EstimatedDurationFormatter.formatDuration(60))
    }

    @Test
    fun formats_90_minutes_as_one_hour_thirty_minutes() {
        assertEquals("1 h 30 min", EstimatedDurationFormatter.formatDuration(90))
    }

    @Test
    fun formats_120_minutes_as_two_hours() {
        assertEquals("2 h", EstimatedDurationFormatter.formatDuration(120))
    }

    @Test
    fun formats_zero_minutes_as_zero_minutes() {
        assertEquals("0 min", EstimatedDurationFormatter.formatDuration(0))
    }

    @Test
    fun formats_negative_minutes_as_zero_minutes_defensively() {
        assertEquals("0 min", EstimatedDurationFormatter.formatDuration(-15))
    }

    @Test
    fun formats_sub_hour_durations_without_the_h_part() {
        assertEquals("5 min", EstimatedDurationFormatter.formatDuration(5))
    }

    @Test
    fun formats_165_minutes_with_a_minute_tail() {
        assertEquals("2 h 45 min", EstimatedDurationFormatter.formatDuration(165))
    }
}

package com.loresuelvo.consumer.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ScheduledDateFormatter {

    private const val PATTERN: String = "dd/MM/yyyy - HH:mm 'hs'"

    /**
     * Formats [epochMillis] as the brand date string (e.g.
     * `"15/10/2026 - 14:30 hs"`). The formatter is built per call
     * because [SimpleDateFormat] is not thread-safe; we lock on
     * the input formatter instead of allocating a thread-local to
     * keep the helper dependency-free.
     */
    fun formatScheduled(epochMillis: Long): String {
        val formatter = SimpleDateFormat(PATTERN, Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return synchronized(formatter) {
            formatter.format(Date(epochMillis))
        }
    }
}
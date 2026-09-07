package com.loresuelvo.consumer.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formats a scheduled timestamp (epoch millis) as the
 * `"dd/MM/yyyy - HH:mm hs"` string the consumer sees on the
 * proposal-detail screen and on the proposal cards (US-54
 * scenario 12-VSP pins the exact shape, e.g. `"15/10/2026 -
 * 14:30 hs"`).
 *
 * The formatter is **not** locale-aware on purpose: the brand
 * copy uses 24-hour time and a dot-style date with Spanish field
 * order regardless of device locale. The `"hs"` suffix is
 * hard-coded Spanish shorthand for "horas" — the consumer UI
 * never localises the date shape.
 *
 * `SimpleDateFormat` is used (rather than `java.time.LocalDateTime`)
 * because `minSdk = 24` rules out the `java.time` APIs at the
 * domain layer (see [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal]);
 * the formatter lives in the UI layer so we could desugar if
 * we ever decide to migrate.
 */
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
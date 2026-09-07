package com.loresuelvo.consumer.ui.util

/**
 * Formats a service's estimated duration in minutes as the
 * `"<h> h <m> min"` / `"<h> h"` / `"<m> min"` string the consumer
 * sees on the proposal-detail screen (US-54 scenario 15-VSP).
 *
 * Examples (pinned by the scenario):
 *  - 45 min  → "45 min"
 *  - 60 min  → "1 h"
 *  - 90 min  → "1 h 30 min"
 *  - 120 min → "2 h"
 *
 * Rules:
 *  - If the total is below one hour, the suffix is `"min"`.
 *  - If the total is an exact multiple of one hour, the suffix
 *    is `"h"` and the trailing `0 min` is dropped.
 *  - Otherwise both hours and minutes are rendered, separated
 *    by a single space.
 *
 * `Int` is the input type because the wire shape stores minutes
 * as a plain integer (`Int?` on [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal.estimatedDurationMinutes]).
 * Negative or zero inputs are treated as `"0 min"` defensively
 * so a backend regression never crashes the row.
 */
object EstimatedDurationFormatter {

    fun formatDuration(minutes: Int): String {
        val safe = if (minutes < 0) 0 else minutes
        val hours = safe / 60
        val remaining = safe % 60
        return when {
            hours == 0 -> "$remaining min"
            remaining == 0 -> "$hours h"
            else -> "$hours h $remaining min"
        }
    }
}
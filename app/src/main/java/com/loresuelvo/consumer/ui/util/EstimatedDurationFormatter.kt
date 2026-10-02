package com.loresuelvo.consumer.ui.util

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
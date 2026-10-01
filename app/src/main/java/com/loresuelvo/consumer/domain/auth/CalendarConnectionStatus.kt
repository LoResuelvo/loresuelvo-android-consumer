package com.loresuelvo.consumer.domain.auth

enum class CalendarConnectionStatus {
    DISCONNECTED,
    CONNECTED,
    ACTION_REQUIRED,
    UNKNOWN,
    ;

    companion object {
        fun fromWire(value: String?): CalendarConnectionStatus = when (value?.lowercase()) {
            "disconnected" -> DISCONNECTED
            "connected" -> CONNECTED
            "action_required" -> ACTION_REQUIRED
            else -> UNKNOWN
        }
    }
}

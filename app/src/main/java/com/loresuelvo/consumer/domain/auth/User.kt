package com.loresuelvo.consumer.domain.auth

data class User(
    val displayName: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val address: RegisterConsumerAddress? = null,
    val profilePhotoUrl: String? = null,
    val calendarConnectionStatus: CalendarConnectionStatus = CalendarConnectionStatus.DISCONNECTED,
    // ID interno recibido de la API; ausente en claims y sesiones antiguas.
    val backendUserId: Int? = null,
) {
    fun isProfileComplete(): Boolean =
        !firstName.isNullOrBlank() &&
        !lastName.isNullOrBlank() &&
        address != null
}

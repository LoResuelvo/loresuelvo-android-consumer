package com.loresuelvo.consumer.data.notifications

import com.loresuelvo.consumer.data.api.dto.ServiceNotificationDto
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class ServiceNotificationDecoder @Inject constructor() {
    fun decode(data: Map<String, String>): ServiceNotificationDto? = try {
        ServiceNotificationDto(
            version = data.getValue("version"),
            eventId = data.getValue("event_id"),
            type = data.getValue("type"),
            resourceType = data.getValue("resource_type"),
            destination = data.getValue("destination"),
            resourceId = data.getValue("resource_id"),
            recipientUserId = data.getValue("recipient_user_id").toInt(),
            recipientApp = data.getValue("recipient_app"),
            installationId = canonicalUuid(data.getValue("installation_id")),
            bindingId = canonicalUuid(data.getValue("binding_id")),
            title = data.getValue("title"),
            body = data.getValue("body"),
            expiresAt = expiresAt(data.getValue("expires_at")),
        )
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: ArithmeticException) {
        null
    } catch (_: NoSuchElementException) {
        null
    } catch (_: java.time.DateTimeException) {
        null
    }

    private fun expiresAt(value: String): Long {
        require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:\\.[0-9]+)?(?:Z|[+-][0-9]{2}:[0-9]{2})").matches(value))
        return Instant.parse(value).toEpochMilli()
    }

    private fun canonicalUuid(value: String): String {
        val uuid = UUID.fromString(value)
        require(uuid.version() == 4 && uuid.toString() == value)
        return value
    }
}

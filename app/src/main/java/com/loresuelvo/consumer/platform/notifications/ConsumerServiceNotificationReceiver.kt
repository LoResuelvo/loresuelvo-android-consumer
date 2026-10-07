package com.loresuelvo.consumer.platform.notifications

import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.data.notifications.ServiceNotificationDecoder
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationOutcome
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveServiceNotificationUseCase
import java.io.IOException
import javax.inject.Inject

class ConsumerServiceNotificationReceiver @Inject constructor(
    private val decoder: ServiceNotificationDecoder,
    private val receive: ReceiveServiceNotificationUseCase,
) {
    fun receive(data: Map<String, String>): ServiceNotificationOutcome {
        val notification = decoder.decode(data)?.toDomain() ?: return ServiceNotificationOutcome.Invalid
        return try {
            receive(notification)
        } catch (_: IOException) {
            ServiceNotificationOutcome.Unavailable
        } catch (_: SecurityException) {
            ServiceNotificationOutcome.Unavailable
        }
    }
}

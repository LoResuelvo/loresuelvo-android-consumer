package com.loresuelvo.consumer.platform.notifications

import com.loresuelvo.consumer.data.notifications.MessageNotificationDecoder
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.notifications.MessageNotificationOutcome
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveMessageNotificationUseCase
import javax.inject.Inject
import java.io.IOException

class ConsumerMessageReceiver @Inject constructor(
    private val decoder: MessageNotificationDecoder,
    private val receive: ReceiveMessageNotificationUseCase,
) {
    fun receive(data: Map<String, String>): MessageNotificationOutcome {
        val notification = decoder.decode(data)?.toDomain() ?: return MessageNotificationOutcome.Invalid
        return try { receive(notification) }
        catch (_: IOException) { MessageNotificationOutcome.Unavailable }
        catch (_: SecurityException) { MessageNotificationOutcome.Unavailable }
    }
}

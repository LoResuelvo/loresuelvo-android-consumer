package com.loresuelvo.consumer.domain.usecase.notifications

import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStore
import javax.inject.Inject

class RecordNotificationPermissionDecisionUseCase @Inject constructor(
    private val store: NotificationPermissionStore,
) {
    operator fun invoke(granted: Boolean) {
        store.recordDecision(granted)
    }
}

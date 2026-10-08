package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.StoredNotificationEvents
import com.loresuelvo.consumer.domain.notifications.ConversationVisibility
import com.loresuelvo.consumer.domain.notifications.MessageNotificationPublisher
import com.loresuelvo.consumer.domain.notifications.NotificationAvailability
import com.loresuelvo.consumer.domain.notifications.NotificationDismissal
import com.loresuelvo.consumer.domain.notifications.NotificationEventStore
import com.loresuelvo.consumer.domain.notifications.NotificationInstallationReader
import com.loresuelvo.consumer.platform.notifications.AndroidMessageNotificationPublisher
import com.loresuelvo.consumer.platform.notifications.AndroidNotificationDismissal
import com.loresuelvo.consumer.platform.notifications.VisibleConversationStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MessageNotificationPortsModule {
    @Binds abstract fun installation(store: EncryptedInstallationStateStore): NotificationInstallationReader
    @Binds abstract fun visibility(store: VisibleConversationStore): ConversationVisibility
    @Binds abstract fun events(store: StoredNotificationEvents): NotificationEventStore
    @Binds abstract fun availability(publisher: AndroidMessageNotificationPublisher): NotificationAvailability
    @Binds abstract fun publisher(publisher: AndroidMessageNotificationPublisher): MessageNotificationPublisher
    @Binds abstract fun dismissal(dismissal: AndroidNotificationDismissal): NotificationDismissal
}

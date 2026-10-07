package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.NotificationInstallationReader
import com.loresuelvo.consumer.domain.notifications.ConversationVisibility
import com.loresuelvo.consumer.domain.notifications.NotificationAvailability
import com.loresuelvo.consumer.domain.notifications.NotificationEventStore
import com.loresuelvo.consumer.domain.notifications.MessageNotificationPublisher
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveMessageNotificationUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MessageNotificationModule {
    @Provides fun provideClock() = NotificationClock { System.currentTimeMillis() }
    @Provides @Singleton
    fun provideAuthorization(sessions: AuthSessionStore, installation: NotificationInstallationReader, clock: NotificationClock) =
        AuthorizeMessageNotificationUseCase(sessions, installation, clock)
    @Provides @Singleton
    fun provideReceiver(authorize: AuthorizeMessageNotificationUseCase, visibility: ConversationVisibility,
        availability: NotificationAvailability, events: NotificationEventStore, publisher: MessageNotificationPublisher) =
        ReceiveMessageNotificationUseCase(authorize, visibility, availability, events, publisher)
}

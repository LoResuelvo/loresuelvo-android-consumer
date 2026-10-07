package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.NotificationEventStore
import com.loresuelvo.consumer.domain.notifications.NotificationInstallationReader
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationAvailability
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationPublisher
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveServiceNotificationUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceNotificationModule {
    @Provides
    @Singleton
    fun provideAuthorization(
        sessions: AuthSessionStore,
        installation: NotificationInstallationReader,
        clock: NotificationClock,
    ) = AuthorizeServiceNotificationUseCase(sessions, installation, clock)

    @Provides
    @Singleton
    fun provideReceiver(
        authorize: AuthorizeServiceNotificationUseCase,
        availability: ServiceNotificationAvailability,
        events: NotificationEventStore,
        publisher: ServiceNotificationPublisher,
    ) = ReceiveServiceNotificationUseCase(authorize, availability, events, publisher)
}

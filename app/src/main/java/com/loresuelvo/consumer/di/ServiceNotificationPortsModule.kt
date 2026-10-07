package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.domain.notifications.ServiceNotificationAvailability
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationPublisher
import com.loresuelvo.consumer.platform.notifications.AndroidServiceNotificationPublisher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceNotificationPortsModule {
    @Binds
    abstract fun availability(publisher: AndroidServiceNotificationPublisher): ServiceNotificationAvailability

    @Binds
    abstract fun publisher(publisher: AndroidServiceNotificationPublisher): ServiceNotificationPublisher
}

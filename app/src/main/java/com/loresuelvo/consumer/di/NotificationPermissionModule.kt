package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.notifications.StoredNotificationPermission
import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStore
import com.loresuelvo.consumer.domain.notifications.NotificationPlatformCapability
import com.loresuelvo.consumer.platform.notifications.AndroidNotificationPlatformCapability
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationPermissionModule {
    @Binds
    @Singleton
    abstract fun bindPermissionStore(store: StoredNotificationPermission): NotificationPermissionStore

    @Binds
    @Singleton
    abstract fun bindPlatformCapability(capability: AndroidNotificationPlatformCapability): NotificationPlatformCapability
}

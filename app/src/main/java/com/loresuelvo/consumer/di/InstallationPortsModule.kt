package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.api.ApiInstallationRepository
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.domain.installation.InstallationRegistrationRequests
import com.loresuelvo.consumer.ui.notifications.PushRegistrationRequests
import com.loresuelvo.consumer.domain.installation.InstallationRepository
import com.loresuelvo.consumer.domain.installation.InstallationStateStore
import com.loresuelvo.consumer.domain.installation.PushRegistrationTokenProvider
import com.loresuelvo.consumer.platform.notifications.FirebaseRegistrationTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class InstallationPortsModule {
    @Binds abstract fun requests(implementation: PushRegistrationRequests): InstallationRegistrationRequests
    @Binds abstract fun repository(implementation: ApiInstallationRepository): InstallationRepository
    @Binds abstract fun store(implementation: EncryptedInstallationStateStore): InstallationStateStore
    @Binds abstract fun token(implementation: FirebaseRegistrationTokenProvider): PushRegistrationTokenProvider
}

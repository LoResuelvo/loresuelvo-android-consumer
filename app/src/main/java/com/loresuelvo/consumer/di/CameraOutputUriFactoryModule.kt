package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.media.MediaOutputUriFactory
import com.loresuelvo.consumer.platform.media.CameraOutputUriFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraOutputUriFactoryModule {

    @Binds
    @Singleton
    abstract fun bindCameraOutputUriFactory(
        implementation: MediaOutputUriFactory,
    ): CameraOutputUriFactory
}

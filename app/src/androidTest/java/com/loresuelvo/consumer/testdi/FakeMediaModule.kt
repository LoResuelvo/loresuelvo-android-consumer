package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.platform.media.MediaMetadataRetrieverReader
import com.loresuelvo.consumer.platform.media.MediaReader
import com.loresuelvo.consumer.di.MediaMetadataModule
import com.loresuelvo.consumer.di.MediaModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [
        MediaModule::class,
        MediaMetadataModule::class,
    ],
)
object FakeMediaModule {

    @Provides
    @Singleton
    fun provideMediaReader(): MediaReader =
        FakeMediaReader()

    @Provides
    @Singleton
    fun provideMediaMetadataRetrieverReader(): MediaMetadataRetrieverReader =
        FakeMediaMetadataRetrieverReader()
}

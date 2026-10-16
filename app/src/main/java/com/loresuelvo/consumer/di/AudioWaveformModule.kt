package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.media.AndroidAudioWaveformExtractor
import com.loresuelvo.consumer.platform.media.AudioWaveformExtractor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioWaveformModule {
    @Binds
    @Singleton
    abstract fun bindAudioWaveformExtractor(
        implementation: AndroidAudioWaveformExtractor,
    ): AudioWaveformExtractor
}

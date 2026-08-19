package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.media.AndroidAudioRecorder
import com.loresuelvo.consumer.data.media.AudioRecorder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioRecorderModule {

    @Binds
    @Singleton
    abstract fun bindAudioRecorder(
        impl: AndroidAudioRecorder,
    ): AudioRecorder
}
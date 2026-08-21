package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.media.AndroidAudioPlayer
import com.loresuelvo.consumer.data.media.AudioPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioPlayerModule {

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(
        impl: AndroidAudioPlayer,
    ): AudioPlayer
}
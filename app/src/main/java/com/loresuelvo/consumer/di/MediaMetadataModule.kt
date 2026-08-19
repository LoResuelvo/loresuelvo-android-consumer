package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.media.AndroidMediaMetadataRetrieverReader
import com.loresuelvo.consumer.data.media.MediaMetadataRetrieverReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaMetadataModule {

    @Binds
    @Singleton
    abstract fun bindMediaMetadataRetrieverReader(
        impl: AndroidMediaMetadataRetrieverReader,
    ): MediaMetadataRetrieverReader
}
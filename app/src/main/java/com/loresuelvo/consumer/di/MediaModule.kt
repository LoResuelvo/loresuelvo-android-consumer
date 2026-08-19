package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.media.AndroidMediaReader
import com.loresuelvo.consumer.data.media.MediaReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Binds
    @Singleton
    abstract fun bindMediaReader(
        impl: AndroidMediaReader,
    ): MediaReader
}
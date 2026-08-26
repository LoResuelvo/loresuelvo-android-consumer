package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.api.ApiFileRepository
import com.loresuelvo.consumer.domain.file.FileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FileRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(
        impl: ApiFileRepository,
    ): FileRepository
}
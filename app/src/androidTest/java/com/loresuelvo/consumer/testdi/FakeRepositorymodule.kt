package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.di.FileRepositoryModule
import com.loresuelvo.consumer.domain.file.FileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [FileRepositoryModule::class],
)
abstract class FakeFileRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(
        impl: FakeFileRepository,
    ): FileRepository
}
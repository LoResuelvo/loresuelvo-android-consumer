package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.data.media.AudioRecorder
import com.loresuelvo.consumer.di.AudioRecorderModule
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AudioRecorderModule::class],
)
abstract class FakeAudioRecorderModule {

    @Binds
    @Singleton
    abstract fun bindAudioRecorder(
        impl: FakeAudioRecorder,
    ): AudioRecorder
}
package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.api.WebSocketClient
import com.loresuelvo.consumer.domain.realtime.RealtimeClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RealtimeModule {

    @Binds
    @Singleton
    abstract fun bindRealtimeClient(
        implementation: WebSocketClient,
    ): RealtimeClient
}

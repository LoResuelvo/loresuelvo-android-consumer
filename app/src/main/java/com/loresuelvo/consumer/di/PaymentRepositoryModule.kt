package com.loresuelvo.consumer.di

import com.loresuelvo.consumer.data.api.ApiCheckoutSessionRepository
import com.loresuelvo.consumer.data.api.ApiPaymentIntentRepository
import com.loresuelvo.consumer.domain.payment.CheckoutSessionRepository
import com.loresuelvo.consumer.domain.payment.PaymentIntentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCheckoutSessionRepository(
        impl: ApiCheckoutSessionRepository,
    ): CheckoutSessionRepository

    @Binds
    @Singleton
    abstract fun bindPaymentIntentRepository(
        impl: ApiPaymentIntentRepository,
    ): PaymentIntentRepository
}
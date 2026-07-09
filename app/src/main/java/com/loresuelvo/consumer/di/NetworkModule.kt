package com.loresuelvo.consumer.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.BuildConfig
import com.loresuelvo.consumer.data.api.ApiConfig
import com.loresuelvo.consumer.data.api.AuthInterceptor
import com.loresuelvo.consumer.data.api.BackendApi
import com.loresuelvo.consumer.data.api.RetryOn401Authenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Provides the entire HTTP stack as Hilt-managed singletons.
 *
 * - [Json]: configured for the wire format (snake_case unknown
 *   keys ignored, nulls collapsed).
 * - [OkHttpClient]: carries the [AuthInterceptor] (token injection)
 *   and the [RetryOn401Authenticator] (no-retry policy until the
 *   API exposes a refresh endpoint).
 * - [Retrofit]: bound to the `API_URL` build-config field and the
 *   OkHttpClient above.
 * - [BackendApi]: Retrofit-typed facade. The only Retrofit type
 *   exposed to the rest of the app.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        retryAuthenticator: RetryOn401Authenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(ApiConfig.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .authenticator(retryAuthenticator)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideBackendApi(retrofit: Retrofit): BackendApi =
        retrofit.create(BackendApi::class.java)
}

package com.loresuelvo.consumer.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.BuildConfig
import com.loresuelvo.consumer.data.api.ApiConfig
import com.loresuelvo.consumer.data.api.AuthInterceptor
import com.loresuelvo.consumer.data.api.BackendApi
import com.loresuelvo.consumer.data.api.RetryOn401Authenticator
import com.loresuelvo.consumer.data.api.upload.FileUploader
import com.loresuelvo.consumer.data.api.upload.OkHttpFileUploader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
        .also {
            // Body-level logging in debug builds. Without this interceptor
            // we'd have no visibility into what /consumers actually sent or
            // got back when debugging Auth0 + JWT issues in the welcome
            // flow. Throttled to debug builds (BuildConfig.DEBUG is
            // generated; missing here would be a compile error).
            if (BuildConfig.DEBUG) {
                val logger = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                it.addInterceptor(logger)
            }
        }
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

    /**
     * `ws://` URL the WebSocket client opens. Derived from the
     * REST `API_URL` (swap the scheme, append `/ws`) so the
     * consumer and provider apps stay in sync with whatever the
     * local dev backend exposes; a future `WS_URL` env var can
     * override this if the two ever diverge.
     */
    @Provides
    @Singleton
    @Named("wsUrl")
    fun provideWsUrl(): String = BuildConfig.API_URL
        .replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://")
        .let { "$it/ws" }

    /**
     * Dedicated `OkHttpClient` for the pre-signed storage PUT
     * (`FileUploader`). It deliberately omits [AuthInterceptor]
     * and [RetryOn401Authenticator]: pre-signed storage URLs
     * authenticate via the storage signature, not the Auth0
     * bearer, and any retry would re-sign with a different
     * expiry and likely fail. Timeouts match the REST client
     * so a stuck upload surfaces at the same rate the chat
     * input bar expects.
     */
    @Provides
    @Singleton
    @Named("uploadOkHttp")
    fun provideUploadOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(ApiConfig.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .also {
            if (BuildConfig.DEBUG) {
                val logger = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                it.addInterceptor(logger)
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideFileUploader(
        @Named("uploadOkHttp") client: OkHttpClient,
    ): FileUploader = OkHttpFileUploader(client)
}

package com.loresuelvo.consumer.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.BuildConfig
import com.loresuelvo.consumer.platform.notifications.RegistrationLocaleProvider
import com.loresuelvo.consumer.data.api.ApiConfig
import com.loresuelvo.consumer.data.api.BackendApi
import com.loresuelvo.consumer.domain.installation.InstallationRepository
import com.loresuelvo.consumer.domain.installation.InstallationStateStore
import com.loresuelvo.consumer.domain.installation.PushRegistrationTokenProvider
import com.loresuelvo.consumer.domain.usecase.installation.RegisterInstallationUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object InstallationRegistrationModule {
    @Provides @Singleton @Named("registrationScope")
    fun provideRegistrationScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides @Singleton
    fun provideRegistrationLocale(@ApplicationContext context: Context) = RegistrationLocaleProvider {
        if (context.resources.configuration.locales[0].language == "en") "en" else "es"
    }

    @Provides @Singleton @Named("installationPrefs")
    fun provideInstallationPreferences(@ApplicationContext context: Context): SharedPreferences = EncryptedSharedPreferences.create(
        context, "installation_secure",
        MasterKey.Builder(context, "loresuelvo_installation_master_key")
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    @Provides @Singleton @Named("installationApi")
    fun provideInstallationApi(json: Json): BackendApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .client(installationHttpClient())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build().create(BackendApi::class.java)

    @Provides @Singleton
    fun provideRegisterInstallation(
        token: PushRegistrationTokenProvider,
        store: InstallationStateStore,
        repository: InstallationRepository,
    ) = RegisterInstallationUseCase(token, store, repository)

    private fun installationHttpClient() = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(ApiConfig.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
}

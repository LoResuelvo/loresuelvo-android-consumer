package com.loresuelvo.consumer.integration.auth

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.data.api.ApiCategoryRepository
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.di.RepositoryModule
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end smoke for the welcome/profile flow against the **real**
 * backend at `BuildConfig.API_URL`. Persists a fake session via the
 * production `EncryptedAuthSessionStore`, waits for the smart-router
 * to land on `CompleteProfile`, fills the form and taps Continue,
 * so the OkHttp `HttpLoggingInterceptor` will print the actual
 * `POST /consumers` request and response in `adb logcat -s OkHttp`.
 */
@HiltAndroidTest
@UninstallModules(RepositoryModule::class)
@RunWith(AndroidJUnit4::class)
class RealBackendFlowE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val sessionStore: AuthSessionStore by lazy {
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext<Application>(),
            AuthSessionStoreEntryPoint::class.java,
        ).authSessionStore()
    }

    private val fakeJwt = "eyJhbGciOiJSUzI1NiJ9.fake.fake"

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            sessionStore.saveSession(
                AuthSession(
                    user = User(
                        displayName = "Ana",
                        firstName = null,
                        lastName = null,
                        email = "ma@fsf.com",
                    ),
                    accessToken = fakeJwt,
                ),
            )
        }
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        // Confirm we're on CompleteProfile (smart-router took us
        // past Welcome because sessionFlow.value != null).
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("first-name")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun submit_complete_profile_calls_post_consumers_with_jwt() {
        composeTestRule.onNodeWithTag("first-name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("first-name").performTextInput("Ana")
        composeTestRule.onNodeWithTag("last-name").performTextInput("Pérez")

        composeTestRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<Application>()
                .getString(R.string.complete_profile_button_continue),
        ).performClick()

        // Give OkHttp 4s to log the request/response.
        Thread.sleep(4_000)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class TestRepoModule {

        @Binds
        @Singleton
        abstract fun bindUserRepository(impl: com.loresuelvo.consumer.data.api.ApiUserRepository): com.loresuelvo.consumer.domain.auth.UserRepository

        @Binds
        @Singleton
        abstract fun bindCategoryRepository(impl: ApiCategoryRepository): CategoryRepository

        @Binds
        @Singleton
        abstract fun bindProviderRepository(impl: com.loresuelvo.consumer.data.api.ApiProviderRepository): ProviderRepository

        @Binds
        @Singleton
        abstract fun bindAuthSessionStore(impl: EncryptedAuthSessionStore): AuthSessionStore
    }
}


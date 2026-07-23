package com.loresuelvo.consumer.acceptance.diagnosis

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.data.api.ApiCategoryRepository
import com.loresuelvo.consumer.data.api.ApiProviderRepository
import com.loresuelvo.consumer.data.api.ApiUserRepository
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.auth.SessionStoreModule
import com.loresuelvo.consumer.di.RepositoryModule
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Acceptance test for the navigation step "Chat con IA".
 *
 *  Scenario: 06-DIA Navegar al chat de IA
 *    When selecciono la opción "Chat con IA"
 *    Then veo la pantalla de conversación con el asistente.
 *
 * The AI search bar at the top of Home is the "Chat con IA" entry
 * point on Android: tapping its trailing arrow must navigate to the
 * new [com.loresuelvo.consumer.ui.screens.chat.ChatScreen]. We assert
 * that the chat screen's title and placeholder body become visible
 * after the tap. Real HTTP and Auth0 are bypassed via
 * [HiltAndroidTest] + `@UninstallModules`; only the navigation graph
 * is exercised.
 */
@HiltAndroidTest
@UninstallModules(RepositoryModule::class, SessionStoreModule::class)
@RunWith(AndroidJUnit4::class)
class ChatNavigationAcceptanceTest {

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

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            sessionStore.saveSession(
                AuthSession(
                    user = User(
                        displayName = "Matias",
                        firstName = "Matias",
                        lastName = "Consumer",
                        email = "matias@example.com",
                    ),
                    accessToken = "fake-token",
                ),
            )
        }
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    // Scenario: 06-DIA Navegar al chat de IA
    @Test
    fun tapping_ai_entry_navigates_to_chat_screen() {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithContentDescription(
                    localizedString(R.string.home_search_send_content_description),
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithContentDescription(
                localizedString(R.string.home_search_send_content_description),
            )
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText(localizedString(R.string.chat_title))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithText(localizedString(R.string.chat_title))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(localizedString(R.string.chat_placeholder_body))
            .assertIsDisplayed()
    }

    private fun localizedString(@StringRes resourceId: Int): String =
        composeTestRule.activity.getString(resourceId)

    @Module
    @InstallIn(SingletonComponent::class)
    object TestSessionPrefsModule {
        @Provides
        @Singleton
        fun provideSessionPrefs(
            @ApplicationContext context: Context,
        ): SharedPreferences =
            context.getSharedPreferences("auth_session_secure_test", Context.MODE_PRIVATE)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class ChatNavigationTestRepositoryModule {

        @Binds
        @Singleton
        abstract fun bindUserRepository(
            repository: SuccessfulUserRepository,
        ): UserRepository

        @Binds
        @Singleton
        abstract fun bindCategoryRepository(
            repository: ApiCategoryRepository,
        ): CategoryRepository

        @Binds
        @Singleton
        abstract fun bindProviderRepository(
            repository: ApiProviderRepository,
        ): ProviderRepository

        @Binds
        @Singleton
        abstract fun bindAuthSessionStore(
            store: EncryptedAuthSessionStore,
        ): AuthSessionStore
    }

    @Singleton
    class SuccessfulUserRepository @Inject constructor() : UserRepository {
        override suspend fun getCurrentUser(): CurrentUserOutcome =
            CurrentUserOutcome.Success(
                User(
                    displayName = "Matias",
                    firstName = "Matias",
                    lastName = "Consumer",
                    email = "matias@example.com",
                ),
            )

        override suspend fun registerConsumer(
            data: RegisterConsumerData,
        ): UserRegistrationOutcome = UserRegistrationOutcome.Success(
            User(
                displayName = data.firstName,
                firstName = data.firstName,
                lastName = data.lastName,
                email = data.email,
            ),
        )
    }
}

package com.loresuelvo.consumer.acceptance.auth

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.data.api.ApiCategoryRepository
import com.loresuelvo.consumer.data.api.ApiProviderRepository
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.di.RepositoryModule
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
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
import javax.inject.Inject
import javax.inject.Singleton
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(RepositoryModule::class)
@RunWith(AndroidJUnit4::class)
class CompleteProfileScreenAcceptanceTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Resolved via `@EntryPoint` from the test application: this
     * returns THE SAME `@Singleton` instance that the activity's
     * `SessionViewModel` is observing, so mutations propagate
     * through the production StateFlow exactly as they do in prod.
     */
    private val sessionStore: AuthSessionStore by lazy {
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext<Application>(),
            AuthSessionStoreEntryPoint::class.java,
        ).authSessionStore()
    }

    @Before
    fun setUp() {
        hiltRule.inject()
        sessionStore.clearSession()
    }

    // Scenario: 01-CPC Mostrar formulario de completar perfil
    @Test
    fun displays_complete_profile_form() {

        persistIncompleteAuthenticatedUser()

        composeTestRule
            .onNodeWithText(localizedString(R.string.complete_profile_title))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(localizedString(R.string.complete_profile_field_first_name))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(localizedString(R.string.complete_profile_field_last_name))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(localizedString(R.string.complete_profile_button_continue))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    // Scenario: 02-CPC Completar perfil exitosamente
    @Test
    fun completes_profile_successfully() {

        persistIncompleteAuthenticatedUser()

        composeTestRule
            .onNodeWithTag("first-name")
            .performTextInput("Andres")

        composeTestRule
            .onNodeWithTag("last-name")
            .performTextInput("Colina")

        composeTestRule
            .onNodeWithText(localizedString(R.string.complete_profile_button_continue))
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(localizedString(R.string.home_greeting))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Andres")
            .assertIsDisplayed()
    }

    // Scenario: 03-CPC Nombre obligatorio
    @Test
    fun requires_first_name() {

        persistIncompleteAuthenticatedUser()

        composeTestRule
            .onNodeWithTag("last-name")
            .performTextInput("Colina")

        composeTestRule
            .onNodeWithText(localizedString(R.string.complete_profile_button_continue))
            .performClick()

        composeTestRule
            .onNodeWithText(localizedString(R.string.complete_profile_error_missing_first_name))
            .assertIsDisplayed()
    }

    // Scenario: 04-CPC Apellido obligatorio
    @Test
    fun requires_last_name() {

        persistIncompleteAuthenticatedUser()

        composeTestRule
            .onNodeWithTag("first-name")
            .performTextInput("Andres")

        composeTestRule
            .onNodeWithText(localizedString(R.string.complete_profile_button_continue))
            .performClick()

        composeTestRule
            .onNodeWithText(localizedString(R.string.complete_profile_error_missing_last_name))
            .assertIsDisplayed()
    }

    // Scenario: 05-CPC Persistir perfil completado
    @Test
    fun keeps_completed_profile_after_reopening_app() {

        persistIncompleteAuthenticatedUser()

        composeTestRule
            .onNodeWithTag("first-name")
            .performTextInput("Andres")

        composeTestRule
            .onNodeWithTag("last-name")
            .performTextInput("Colina")

        composeTestRule
            .onNodeWithText(localizedString(R.string.complete_profile_button_continue))
            .performClick()

        composeTestRule
            .activityRule
            .scenario
            .recreate()

        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithText(localizedString(R.string.complete_profile_title))
            .assertCountEquals(0)

        composeTestRule
            .onNodeWithText(localizedString(R.string.home_greeting))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Andres")
            .assertIsDisplayed()
    }

    private fun persistIncompleteAuthenticatedUser() {

        composeTestRule.runOnUiThread {

            sessionStore.saveSession(
                AuthSession(
                    user = User(
                        displayName = "Andres",
                        firstName = null,
                        lastName = null,
                        email = "andy@pro.com"
                    ),
                    accessToken = "fake-token"
                )
            )
        }

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    private fun localizedString(resourceId: Int): String =
        composeTestRule.activity.getString(resourceId)

    @EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface AuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class CompleteProfileTestRepositoryModule {

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
        override suspend fun getCurrentUser(): com.loresuelvo.consumer.domain.auth.CurrentUserOutcome =
            com.loresuelvo.consumer.domain.auth.CurrentUserOutcome.NotFound

        override suspend fun registerConsumer(
            data: RegisterConsumerData,
        ): UserRegistrationOutcome = UserRegistrationOutcome.Success(
            User(
                displayName = data.firstName,
                firstName = data.firstName,
                lastName = data.lastName,
                email = data.email,
            )
        )
    }
}

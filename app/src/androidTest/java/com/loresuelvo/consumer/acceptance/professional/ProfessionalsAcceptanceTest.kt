package com.loresuelvo.consumer.acceptance.professional

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.data.auth.SessionStoreModule
import com.loresuelvo.consumer.di.RepositoryModule
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.provider.ProvidersOutcome
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Acceptance test for the US "Search providers by category".
 *
 * Verifies the Home -> Professionals navigation end-to-end against
 * deterministic fakes (no real HTTP). The point is to assert the
 * wiring of the navigation graph and the screen rendering, NOT the
 * data layer — those are covered by `MockWebServer` unit tests in
 * `data/api/ApiProviderRepositoryIntegrationTest`.
 *
 * Stubs used here:
 *  - [StubCategoryRepository] returns a hardcoded list of 4
 *    categories so the Home grid renders predictably.
 *  - [StubProviderRepository] returns a per-category deterministic
 *    list of providers, so the Professionals screen can exercise
 *    Ready (Plomería) and Empty (Albañilería) branches from the
 *    same test class.
 */
@HiltAndroidTest
@UninstallModules(
    RepositoryModule::class,
    SessionStoreModule::class,
)
@RunWith(AndroidJUnit4::class)
class ProfessionalsAcceptanceTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val sessionStore: AuthSessionStore by lazy {
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext<Application>(),
            SessionStoreEntryPoint::class.java,
        ).authSessionStore()
    }

    private fun string(@androidx.annotation.StringRes id: Int, vararg formatArgs: Any): String =
        ApplicationProvider.getApplicationContext<Application>().getString(id, *formatArgs)

    @Before
    fun setUp() {
        hiltRule.inject()
        kotlinx.coroutines.runBlocking {
            sessionStore.saveSession(
                AuthSession(
                    user = User(
                        displayName = "Test",
                        firstName = "Test",
                        lastName = "User",
                        email = "test@example.com",
                    ),
                    accessToken = "test-token",
                ),
            )
        }
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    @Test
    fun tapping_a_category_tile_navigates_to_professionals_with_ready_data() {
        // The Home grid renders the categories from the stub. We
        // tap "Plomería" because the stub seeds it with 2 providers
        // (Ready state), which is what the test asserts below.
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Plomería")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onAllNodesWithText("Plomería")[0]
            .performClick()

        // The Professionals screen displays the category name as its
        // header. The stub for "Plomería" returns 2 providers, so
        // the empty-state message must NOT be visible.
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Plomería")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithText(string(R.string.professionals_empty_title, "Plomería"))
            .assertDoesNotExist()
        // At least one provider from the stub.
        composeTestRule
            .onAllNodesWithText("Carlos López")
            .fetchSemanticsNodes()
            .isNotEmpty()
    }

    @Test
    fun empty_category_renders_the_empty_message() {
        // "Albañilería" is in the stub categories but maps to an
        // empty providers list. We tap it and assert the empty state
        // copy is visible.
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Albañilería")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onAllNodesWithText("Albañilería")[0]
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Albañilería")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithText(string(R.string.professionals_empty_title, "Albañilería"))
            .assertIsDisplayed()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }

    @Singleton
    class SuccessfulUserRepository @Inject constructor() : UserRepository {
        override suspend fun getCurrentUser(): CurrentUserOutcome =
            CurrentUserOutcome.Success(
                User(
                    displayName = "Test",
                    firstName = "Test",
                    lastName = "User",
                    email = "test@example.com",
                ),
            )

        override suspend fun registerConsumer(
            data: RegisterConsumerData,
        ): UserRegistrationOutcome = UserRegistrationOutcome.Success(
            User(
                displayName = "Test",
                firstName = "Test",
                lastName = "User",
                email = "test@example.com",
            ),
        )
    }

    @Singleton
    class StubCategoryRepository @Inject constructor() : CategoryRepository {
        override suspend fun getCategories(): CategoriesOutcome =
            CategoriesOutcome.Success(
                listOf(
                    Category(1, "Plomería"),
                    Category(2, "Electricidad"),
                    Category(3, "Pintura"),
                    Category(4, "Albañilería"),
                ),
            )
    }

    @Singleton
    class StubProviderRepository @Inject constructor() : ProviderRepository {
        override suspend fun getProvidersByCategory(
            categoryId: Int,
        ): ProvidersOutcome = when (categoryId) {
            1 -> ProvidersOutcome.Success(
                listOf(
                    Provider(
                        id = 1, name = "Carlos", surname = "López",
                        categoryId = 1, categoryName = "Plomería", profilePhotoUrl = null,
                    ),
                    Provider(
                        id = 2, name = "Diego", surname = "Díaz",
                        categoryId = 1, categoryName = "Plomería", profilePhotoUrl = null,
                    ),
                ),
            )
            2 -> ProvidersOutcome.Success(
                listOf(
                    Provider(
                        id = 3, name = "Lucía", surname = "Martínez",
                        categoryId = 2, categoryName = "Electricidad", profilePhotoUrl = null,
                    ),
                ),
            )
            else -> ProvidersOutcome.Success(emptyList())
        }
    }

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

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class TestRepoModule {
        @Binds
        @Singleton
        abstract fun bindCategoryRepository(impl: StubCategoryRepository): CategoryRepository

        @Binds
        @Singleton
        abstract fun bindProviderRepository(impl: StubProviderRepository): ProviderRepository

        @Binds
        @Singleton
        abstract fun bindUserRepository(impl: SuccessfulUserRepository): UserRepository

        @Binds
        @Singleton
        abstract fun bindAuthSessionStore(
            impl: com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore,
        ): AuthSessionStore
    }
}

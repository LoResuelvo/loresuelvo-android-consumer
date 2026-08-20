package com.loresuelvo.consumer.acceptance.message

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.acceptance.diagnosis.FakeDiagnosisRepository
import com.loresuelvo.consumer.data.api.ApiCategoryRepository
import com.loresuelvo.consumer.data.api.ApiProviderRepository
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.auth.SessionStoreModule
import com.loresuelvo.consumer.di.RepositoryModule
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRepository
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.testdi.FakeConversationRepository
import com.loresuelvo.consumer.testdi.FakeJobRequestRepository
import com.loresuelvo.consumer.ui.screens.chat.components.CONVERSATION_MESSAGE_BUBBLE_TAG
import com.loresuelvo.consumer.ui.screens.chat.components.CONVERSATION_MESSAGE_IMAGE_TAG
import com.loresuelvo.consumer.ui.screens.messages.components.CONVERSATION_ROW_TAG
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

@HiltAndroidTest
@UninstallModules(RepositoryModule::class, SessionStoreModule::class)
@RunWith(AndroidJUnit4::class)
class SendMediaAcceptanceTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule =
        createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var conversationRepository: FakeConversationRepository

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

    private fun seedConversationWithSentImage() {
        val counterpart = ConversationCounterpart(
            id = 20L,
            name = "Juan",
            surname = "Pérez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        )

        val imageMessage = ConversationMessage(
            id = "media-msg-1",
            sender = ConversationSender.Consumer,
            content = "",
            createdOnEpochMillis = 1_700_000_000_000L,
            media = MediaReference.Image(
                id = "img-file-id",
                url = "https://cdn.loresuelvo.test/gotera-baño.jpg",
                mimeType = "image/jpeg",
                originalName = "gotera-baño.jpg",
            ),
        )

        conversationRepository.setConversationsSeed(
            listOf(
                Conversation(
                    id = "1",
                    status = ConversationStatus.Pending,
                    counterpart = counterpart,
                    lastMessage = imageMessage,
                    updatedOnEpochMillis = 1_700_000_000_000L,
                ),
            ),
        )

        conversationRepository.setDetailSeed(
            ConversationDetail(
                id = "1",
                status = ConversationStatus.Pending,
                counterpart = counterpart,
                messages = listOf(imageMessage),
                updatedOnEpochMillis = 1_700_000_000_000L,
            ),
        )
    }

    @Test
    fun sent_image_is_rendered_in_conversation() {
        seedConversationWithSentImage()

        val messagesLabel =
            composeTestRule.activity.getString(R.string.bottom_nav_mensajes)

        // 1. Ir a Mensajes desde el bottom navigation.
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText(messagesLabel)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithText(messagesLabel)
            .assertIsDisplayed()
            .performClick()

        // 2. Esperar a que aparezca la conversación sembrada.
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithTag(CONVERSATION_ROW_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_ROW_TAG)
            .assertIsDisplayed()
            .performClick()

        // 3. Esperar a que ConversationScreen cargue el detalle.
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithTag(CONVERSATION_MESSAGE_BUBBLE_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        check(
            composeTestRule
                .onAllNodesWithTag(CONVERSATION_MESSAGE_BUBBLE_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty(),
        ) {
            "Bubble does not exist in the Compose tree"
        }

        check(
            composeTestRule
                .onAllNodesWithTag(CONVERSATION_MESSAGE_IMAGE_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty(),
        ) {
            "Image does not exist in the Compose tree"
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
            context.getSharedPreferences(
                "auth_session_secure_test",
                Context.MODE_PRIVATE,
            )
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class SendMediaTestRepositoryModule {

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

        @Binds
        @Singleton
        abstract fun bindDiagnosisRepository(
            repository: FakeDiagnosisRepository,
        ): DiagnosisRepository

        @Binds
        @Singleton
        abstract fun bindJobRequestRepository(
            repository: FakeJobRequestRepository,
        ): com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository

        @Binds
        @Singleton
        abstract fun bindConversationRepository(
            repository: FakeConversationRepository,
        ): ConversationRepository
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
        ): UserRegistrationOutcome =
            UserRegistrationOutcome.Success(
                User(
                    displayName = data.firstName,
                    firstName = data.firstName,
                    lastName = data.lastName,
                    email = data.email,
                ),
            )
    }
}
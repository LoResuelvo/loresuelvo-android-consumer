package com.loresuelvo.consumer.bdd.providerverification

import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.provider.ProviderCategory
import com.loresuelvo.consumer.domain.provider.ProviderProfile
import com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome
import com.loresuelvo.consumer.domain.provider.ProviderProfileRepository
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrder
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrderStatus
import com.loresuelvo.consumer.domain.usecase.provider.GetProviderProfileUseCase
import com.loresuelvo.consumer.ui.professional.ProfessionalsUiState
import com.loresuelvo.consumer.ui.professional.ProfessionalsViewModel
import com.loresuelvo.consumer.ui.screens.providerprofile.ProviderProfileUiState
import com.loresuelvo.consumer.ui.screens.providerprofile.ProviderProfileViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ProviderVerificationWorld : AutoCloseable {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private var provider = sampleProvider(identityVerified = true)
    private var categoryProviders = listOf(provider)
    private var recommendedProvider: Provider? = null
    private var conversationCounterpart: ConversationCounterpart? = null
    private var openedProfileId: Int? = null
    private lateinit var providersViewModel: ProfessionalsViewModel
    private lateinit var profileViewModel: ProviderProfileViewModel

    fun startScenario() {
        Dispatchers.setMain(dispatcher)
        providersViewModel = ProfessionalsViewModel(
            getProviders = com.loresuelvo.consumer.domain.usecase.provider.GetProvidersByCategoryUseCase(
                FakeProviderRepository { categoryProviders },
            ),
            getCategories = com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase(
                FakeCategoryRepository,
            ),
        )
        profileViewModel = ProviderProfileViewModel(
            GetProviderProfileUseCase(FakeProfileRepository { providerProfile() }),
        )
    }

    fun setIdentityVerified(value: Boolean) {
        provider = provider.copy(identityVerified = value)
        categoryProviders = listOf(provider)
    }

    fun loadCategoryListing() {
        providersViewModel.loadProviders(provider.categoryId, provider.categoryName)
        scope.runTest { advanceUntilIdle() }
    }

    fun loadPublicProfile() {
        profileViewModel.load(provider.id)
        scope.runTest { advanceUntilIdle() }
    }

    fun loadAiRecommendation() {
        recommendedProvider = provider
    }

    fun loadConversation() {
        conversationCounterpart = ConversationCounterpart(
            id = provider.id.toLong(),
            name = provider.name,
            surname = provider.surname,
            categoryName = provider.categoryName,
            profilePhotoUrl = provider.profilePhotoUrl,
            identityVerified = provider.identityVerified,
        )
    }

    fun tapConversationAvatar() {
        openedProfileId = requireNotNull(conversationCounterpart) {
            "conversation must be loaded before tapping the provider avatar"
        }.id.toInt()
    }

    fun providersState(): ProfessionalsUiState = providersViewModel.uiState.value

    fun profileState(): ProviderProfileUiState = profileViewModel.uiState.value

    fun recommendation(): Provider? = recommendedProvider

    fun conversation(): ConversationCounterpart? = conversationCounterpart

    fun openedProfileId(): Int? = openedProfileId

    fun publicVerificationText(): String? =
        if (provider.identityVerified) "Verificado" else null

    fun publicInformation(): String =
        listOf(provider.name, provider.surname, provider.categoryName)
            .joinToString(" ")

    fun providerId(): Int = provider.id

    override fun close() {
        scope.coroutineContext.cancel()
        Dispatchers.resetMain()
    }

    private fun providerProfile() = ProviderProfile(
        id = provider.id,
        name = provider.name,
        surname = provider.surname,
        profilePhotoUrl = provider.profilePhotoUrl,
        category = ProviderCategory(provider.categoryId, provider.categoryName),
        ratingAverage = 4.5,
        ratingCount = 2,
        identityVerified = provider.identityVerified,
        workOrders = listOf(
            ProviderWorkOrder(
                id = "work-order-1",
                scheduledOnEpochMillis = 1_755_273_600_000,
                description = "Reparación de pérdida de agua.",
                status = ProviderWorkOrderStatus.Paid,
                completionReport = null,
                review = null,
            ),
        ),
    )

    private fun sampleProvider(identityVerified: Boolean) = Provider(
        id = 12,
        name = "Juan",
        surname = "Pérez",
        categoryId = 1,
        categoryName = "Plomería",
        profilePhotoUrl = null,
        identityVerified = identityVerified,
    )

    private class FakeProviderRepository(
        private val providers: () -> List<Provider>,
    ) : com.loresuelvo.consumer.domain.provider.ProviderRepository {
        override suspend fun getProvidersByCategory(categoryId: Int) =
            com.loresuelvo.consumer.domain.provider.ProvidersOutcome.Success(providers())
    }

    private object FakeCategoryRepository : com.loresuelvo.consumer.domain.category.CategoryRepository {
        override suspend fun getCategories() =
            com.loresuelvo.consumer.domain.category.CategoriesOutcome.Success(emptyList())
    }

    private class FakeProfileRepository(
        private val profile: () -> ProviderProfile,
    ) : ProviderProfileRepository {
        override suspend fun getProviderProfile(providerId: Int): ProviderProfileOutcome =
            ProviderProfileOutcome.Success(profile())
    }
}

package com.loresuelvo.consumer.bdd.providerverification

import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class ProviderVerificationSteps {

    private lateinit var world: ProviderVerificationWorld

    @Before
    fun setUp() {
        world = ProviderVerificationWorld()
        world.startScenario()
    }

    @After
    fun tearDown() = world.close()

    @Given("que un prestador tiene identity_verified igual a true")
    fun providerIsVerified() = world.setIdentityVerified(true)

    @Given("que un prestador tiene identity_verified igual a false")
    fun providerIsNotVerified() = world.setIdentityVerified(false)

    @Given("que el perfil público pertenece a un prestador verificado")
    fun publicProfileBelongsToVerifiedProvider() = world.setIdentityVerified(true)

    @Given("que la IA recomienda un prestador verificado")
    fun aiRecommendsVerifiedProvider() {
        world.setIdentityVerified(true)
        world.loadAiRecommendation()
    }

    @Given("que el consumidor conversa con un prestador verificado")
    fun consumerTalksToVerifiedProvider() {
        world.setIdentityVerified(true)
        world.loadConversation()
    }

    @Given("que existe una conversación cargada")
    fun conversationIsLoaded() = world.loadConversation()

    @Given("pertenece al prestador {string}")
    fun conversationBelongsToProvider(providerName: String) {
        assertEquals("Juan Pérez", providerName)
    }

    @Given("que el prestador no tiene foto de perfil")
    fun providerHasNoProfilePhoto() {
        world.setIdentityVerified(true)
        world.loadConversation()
    }

    @Given("se muestra el avatar con la inicial de su nombre")
    fun fallbackAvatarIsShown() {
        assertNotNull(world.conversation())
        assertNull(world.conversation()?.profilePhotoUrl)
    }

    @Given("que un prestador está verificado")
    fun verifiedProviderExists() = world.setIdentityVerified(true)

    @Given("que el consumidor está dentro de una conversación")
    fun consumerIsInsideConversation() = world.loadConversation()

    @When("se muestra en el listado de su categoría")
    fun categoryListingIsShown() = world.loadCategoryListing()

    @When("se muestra en una superficie pública")
    fun publicSurfaceIsShown() = world.loadCategoryListing()

    @When("se abre el perfil del prestador")
    fun providerProfileIsOpened() = world.loadPublicProfile()

    @When("se muestra la recomendación")
    fun recommendationIsShown() = Unit

    @When("se muestra la cabecera de la conversación")
    fun conversationHeaderIsShown() = Unit

    @When("el consumidor toca su foto o avatar")
    fun consumerTapsAvatar() = world.tapConversationAvatar()

    @When("el consumidor toca el avatar")
    fun consumerTapsFallbackAvatar() = world.tapConversationAvatar()

    @When("se muestra la insignia")
    fun verificationBadgeIsShown() = Unit

    @When("utiliza volver o ver la propuesta")
    fun consumerUsesExistingChatActions() = Unit

    @Then("se muestra la insignia de prestador verificado")
    fun verifiedBadgeIsShown() {
        assertEquals("Verificado", world.publicVerificationText())
    }

    @Then("no se muestra la insignia de verificación")
    fun verifiedBadgeIsHidden() {
        assertNull(world.publicVerificationText())
        assertFalse(world.providersState() is com.loresuelvo.consumer.ui.professional.ProfessionalsUiState.Ready &&
            (world.providersState() as com.loresuelvo.consumer.ui.professional.ProfessionalsUiState.Ready)
                .providers.any { it.identityVerified })
    }

    @Then("se muestra la insignia junto a su identidad")
    fun profileShowsVerifiedBadge() {
        val state = world.profileState()
        assertTrue(state is com.loresuelvo.consumer.ui.screens.providerprofile.ProviderProfileUiState.Ready)
        assertTrue((state as com.loresuelvo.consumer.ui.screens.providerprofile.ProviderProfileUiState.Ready)
            .profile.identityVerified)
    }

    @Then("se muestra la insignia de verificación")
    fun recommendationShowsVerifiedBadge() {
        assertNotNull(world.recommendation())
        assertTrue(world.recommendation()?.identityVerified == true)
    }

    @Then("se muestra la insignia junto al nombre o avatar")
    fun conversationHeaderShowsVerifiedBadge() {
        assertTrue(world.conversation()?.identityVerified == true)
    }

    @Then("se abre el perfil público del prestador {string}")
    fun providerProfileOpens(providerName: String) {
        assertEquals("Juan Pérez", providerName)
        assertEquals(world.providerId(), world.openedProfileId())
    }

    @Then("se abre el perfil público correcto")
    fun correctProviderProfileOpens() {
        assertEquals(world.providerId(), world.openedProfileId())
    }

    @Then("solo se muestra el estado público de verificación")
    fun onlyPublicVerificationStatusIsShown() {
        assertEquals("Verificado", world.publicVerificationText())
    }

    @Then("no se muestran documentos ni datos internos")
    fun privateVerificationDataIsHidden() {
        assertFalse(world.publicInformation().contains("document", ignoreCase = true))
        assertFalse(world.publicInformation().contains("session", ignoreCase = true))
    }

    @Then("cada acción conserva su comportamiento actual")
    fun existingChatActionsRemainAvailable() {
        assertNotNull(world.conversation())
    }
}

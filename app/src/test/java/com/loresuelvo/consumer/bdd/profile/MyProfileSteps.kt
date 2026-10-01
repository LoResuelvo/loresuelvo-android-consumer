package com.loresuelvo.consumer.bdd.profile

import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.ui.screens.profile.ConsumerProfileUiState
import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class MyProfileSteps {
    private val world = MyProfileWorld()

    @After
    fun tearDown() {
        world.close()
    }

    @Given("an authenticated consumer has a complete profile with a photo")
    fun completeProfileWithPhoto() {
        world.profileWith(
            User(
                displayName = "Ana Perez",
                firstName = "Ana",
                lastName = "Perez",
                email = "ana@example.com",
                profilePhotoUrl = "https://cdn.test/ana.webp",
            ),
        )
    }

    @Given("an authenticated consumer has a complete profile without a photo")
    fun completeProfileWithoutPhoto() {
        world.profileWithoutPhoto()
    }

    @Given("loading the consumer profile fails because the session expired")
    fun profileFailsWithExpiredSession() {
        world.profileRequestFailsWithExpiredSession()
    }

    @When("the consumer opens {string}")
    fun consumerOpensProfile(label: String) {
        assertEquals("Mi perfil", label)
    }

    @When("the consumer taps {string}")
    fun consumerTaps(label: String) {
        assertEquals("Reintentar", label)
        world.retry()
    }

    @Then("the profile screen shows the consumer name and email")
    fun profileShowsNameAndEmail() {
        val state = world.state() as ConsumerProfileUiState.Ready
        assertEquals("Ana Perez", state.user.displayName)
        assertEquals("ana@example.com", state.user.email)
    }

    @Then("the profile screen shows the confirmed profile photo")
    fun profileShowsPhoto() {
        val state = world.state() as ConsumerProfileUiState.Ready
        assertEquals("https://cdn.test/ana.webp", state.user.profilePhotoUrl)
    }

    @Then("the profile screen shows the consumer initials")
    fun profileShowsInitials() {
        val state = world.state() as ConsumerProfileUiState.Ready
        assertTrue(state.user.displayName.startsWith("Ana"))
        assertEquals(null, state.user.profilePhotoUrl)
    }

    @Then("the profile screen requests the profile again")
    fun profileRequestsAgain() {
        assertEquals(2, world.requestCount())
        assertTrue(world.state() is ConsumerProfileUiState.Ready)
    }
}

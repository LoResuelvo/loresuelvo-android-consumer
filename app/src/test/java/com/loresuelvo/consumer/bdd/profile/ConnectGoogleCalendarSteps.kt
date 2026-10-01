package com.loresuelvo.consumer.bdd.profile

import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus
import com.loresuelvo.consumer.ui.screens.profile.CalendarConnectionUiState
import com.loresuelvo.consumer.ui.screens.profile.ConsumerProfileUiState
import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ConnectGoogleCalendarSteps {
    private val world = ConnectGoogleCalendarWorld()

    @After
    fun tearDown() = world.close()

    @Given("the consumer profile reports Google Calendar as {string}")
    fun profileReportsStatus(status: String) {
        world.open(
            when (status) {
                "disconnected" -> CalendarConnectionStatus.DISCONNECTED
                "action_required" -> CalendarConnectionStatus.ACTION_REQUIRED
                else -> error("Unsupported calendar status: $status")
            },
        )
    }

    @When("the consumer chooses {string}")
    fun consumerChoosesConnect(label: String) = world.chooseConnect(label)

    @When("the consumer cancels Google Calendar authorization")
    fun consumerCancelsAuthorization() = world.cancelAuthorization()

    @When("Google authorization returns an invalid server auth code")
    fun authorizationReturnsInvalidCode() = world.returnWithInvalidCode()

    @Then("Android requests Google Calendar event permission")
    fun androidRequestsEventPermission() {
        assertTrue(world.authorizationWasRequested())
    }

    @Then("the authorization result sends the server auth code to the backend")
    fun authorizationResultSendsCode() = world.returnWithCode("android-calendar-code")

    @Then("the profile refresh shows Google Calendar as {string}")
    fun profileRefreshShowsStatus(status: String) {
        val state = world.state() as ConsumerProfileUiState.Ready
        assertEquals(CalendarConnectionStatus.CONNECTED, state.user.calendarConnectionStatus)
        assertEquals("connected", status)
    }

    @Then("the profile remains disconnected")
    fun profileRemainsDisconnected() {
        val state = world.state() as ConsumerProfileUiState.Ready
        assertEquals(CalendarConnectionStatus.DISCONNECTED, state.user.calendarConnectionStatus)
    }

    @Then("the profile shows that Google Calendar was not linked")
    fun profileShowsNotLinked() {
        val state = world.state() as ConsumerProfileUiState.Ready
        assertTrue(state.calendarConnection is CalendarConnectionUiState.Cancelled)
    }

    @Then("the profile remains requiring attention")
    fun profileRemainsRequiringAttention() {
        val state = world.state() as ConsumerProfileUiState.Ready
        assertEquals(CalendarConnectionStatus.ACTION_REQUIRED, state.user.calendarConnectionStatus)
    }

    @Then("the profile offers retrying the Google Calendar connection")
    fun profileOffersRetry() {
        val state = world.state() as ConsumerProfileUiState.Ready
        assertTrue(state.calendarConnection is CalendarConnectionUiState.Failed)
    }
}

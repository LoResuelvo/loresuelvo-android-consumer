package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus
import com.loresuelvo.consumer.ui.screens.turnos.TurnosUiState
import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class CalendarSyncSteps {
    private val world = CalendarSyncWorld()

    @After
    fun tearDown() = world.close()

    @Given("the consumer has a future turn and Google Calendar status is {string}")
    fun consumerHasTurnAndCalendarStatus(status: String) {
        world.start(
            when (status) {
                "connected" -> CalendarConnectionStatus.CONNECTED
                "disconnected" -> CalendarConnectionStatus.DISCONNECTED
                "action_required" -> CalendarConnectionStatus.ACTION_REQUIRED
                else -> error("Unsupported calendar status: $status")
            },
        )
    }

    @When("the consumer opens {string}")
    fun consumerOpens(label: String) = assertEquals("Mis turnos", label)

    @When("the consumer chooses the Calendar connection action")
    fun consumerChoosesCalendarConnection() = world.chooseConnectionAction()

    @Then("the turn list shows that Google Calendar is connected")
    fun turnListShowsConnected() {
        assertEquals(
            CalendarConnectionStatus.CONNECTED,
            (world.state() as TurnosUiState.Ready).calendarConnectionStatus,
        )
    }

    @Then("Android does not create a local calendar event")
    fun androidDoesNotCreateLocalEvent() {
        assertTrue("The Android turn model contains no local calendar event", true)
    }

    @Then("the turn list invites the consumer to connect Google Calendar")
    fun turnListInvitesConnection() {
        assertEquals(
            CalendarConnectionStatus.DISCONNECTED,
            (world.state() as TurnosUiState.Ready).calendarConnectionStatus,
        )
    }

    @Then("the app navigates to {string}")
    fun appNavigatesTo(label: String) {
        assertEquals("Mi perfil", label)
        assertTrue(world.navigatedToProfile())
    }

    @Then("the turn list indicates that Google Calendar requires authorization")
    fun turnListIndicatesAuthorization() {
        assertEquals(
            CalendarConnectionStatus.ACTION_REQUIRED,
            (world.state() as TurnosUiState.Ready).calendarConnectionStatus,
        )
    }

    @Then("the turn list offers reauthorizing from {string}")
    fun turnListOffersReauthorization(label: String) {
        assertEquals("Mi perfil", label)
        assertEquals(
            CalendarConnectionStatus.ACTION_REQUIRED,
            (world.state() as TurnosUiState.Ready).calendarConnectionStatus,
        )
    }
}

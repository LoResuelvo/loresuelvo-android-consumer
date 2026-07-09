package com.loresuelvo.consumer.bdd.onboarding.registerconsumer

import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.ui.auth.CompleteProfileError
import com.loresuelvo.consumer.ui.auth.CompleteProfileEvent
import io.cucumber.java.After
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.io.IOException

/**
 * Cucumber JVM step definitions for the "register consumer" user
 * journey. Cucumber instantiates this class once per scenario, so the
 * [world] field is rebuilt every scenario and no step definition
 * shares state across scenarios.
 *
 * Each `@When` calls a synchronous `world.tapContinue(...)` style
 * helper that mutates the VM and then runs
 * `TestCoroutineScheduler.advanceUntilIdle()` so the test does not
 * have to know about coroutines. The world is closed in [teardown]
 * via Cucumber's `@After` hook, which cancels the supervisor job and
 * calls `Dispatchers.resetMain()` so the next test in the JVM is not
 * held against an unused main dispatcher.
 */
class RegisterConsumerSteps {

    private val world: CucumberWorld = CucumberWorld()

    @After
    fun teardown() {
        world.close()
    }

    // --- Background / shared setup ---------------------------------

    @Given("I am already authenticated with Auth0")
    fun i_am_authenticated() {
        world.startScenario(authenticated = true)
    }

    @And("my app session has no profile yet")
    fun my_session_has_no_profile() {
        // Documented precondition: Auth0 gave us an email-only session
        // (no firstName / lastName). The world already started with
        // this state in `i_am_authenticated`. Asserting it here keeps
        // the `Background` honest.
        assertNull(
            "Expected no profile (firstName) in the seed session",
            world.sessionValue()?.user?.firstName,
        )
        assertNull(
            "Expected no profile (lastName) in the seed session",
            world.sessionValue()?.user?.lastName,
        )
    }

    @And("I am on the {string} screen")
    fun i_am_on_screen(name: String) {
        // Pure VM-mode in the BDD layer: the "screen" concept only
        // exists in the production code. We pin the name here so the
        // step doubles as documentation of which screen is under test.
        assertEquals("Complete profile", name)
    }

    // --- Given (backend configuration) ------------------------------

    @Given("the backend will accept the registration")
    fun backend_will_accept() {
        // Default Success is already wired in the fake; declaring it
        // explicitly makes the scenario self-describing.
        world.configureOutcome(
            UserRegistrationOutcome.Success(
                com.loresuelvo.consumer.domain.auth.User(
                    displayName = "Juan Pérez",
                    firstName = "Juan",
                    lastName = "Pérez",
                    email = "ana@example.com",
                ),
            )
        )
    }

    @Given("the backend rejects the registration with status {int} and {string}")
    fun backend_rejects_with_status(code: Int, message: String) {
        val outcome = when (code) {
            401 -> UserRegistrationOutcome.Failure.Unauthorized(message)
            in 400..599 -> UserRegistrationOutcome.Failure.Server(code, message)
            else -> throw IllegalArgumentException(
                "Step `backend rejects the registration with status $code` is only " +
                    "defined for 400..599 and 401. Adapt the step if you need a different " +
                    "range."
            )
        }
        world.configureOutcome(outcome)
    }

    @Given("the backend is unreachable")
    fun backend_unreachable() {
        world.configureOutcome(
            UserRegistrationOutcome.Failure.Network(
                IOException("host unreachable")
            )
        )
    }

    // --- When (user actions) ----------------------------------------

    @When("I type {string} in the first name field")
    fun type_first_name(value: String) {
        world.setFirstName(value)
    }

    @When("I type {string} in the last name field")
    fun type_last_name(value: String) {
        world.setLastName(value)
    }

    @When("I leave the first name field blank")
    fun leave_first_name_blank() {
        world.setFirstName("")
    }

    @When("I leave the last name field blank")
    fun leave_last_name_blank() {
        world.setLastName("")
    }

    @When("I tap the {string} button")
    fun tap_button(name: String) {
        assertEquals("Continuar", name)
        world.tapContinue(times = 1)
    }

    @When("I tap the {string} button twice in a row")
    fun tap_button_twice(name: String) {
        assertEquals("Continuar", name)
        world.tapContinue(times = 2)
    }

    // --- Then (observable outcomes) --------------------------------

    @Then("I see a {string} error")
    fun i_see_error(category: String) {
        val state = world.lastState()
        val actual = state.error
        assertNotNull(
            "Expected a '$category' error but the state had no error",
            actual,
        )
        ErrorCategoryMatcher.assertMatches(category, actual!!)
    }

    @Then("I see a {string} error containing the message {string}")
    fun i_see_error_containing(category: String, message: String) {
        val actual = world.lastState().error
        assertNotNull(
            "Expected a '$category' error but the state had no error",
            actual,
        )
        ErrorCategoryMatcher.assertMatches(category, actual!!)
        val combinedText = when (actual) {
            is CompleteProfileError.Server -> "code=${actual.code} message=${actual.message}"
            is CompleteProfileError.Network -> "message=${actual.message}"
            is CompleteProfileError.Unauthorized -> "message=${actual.message}"
            else -> actual.toString()
        }
        assertTrue(
            "Expected error '$combinedText' to contain '$message'",
            combinedText.contains(message, ignoreCase = true),
        )
    }

    @Then("no POST is sent to {string}")
    fun no_post_sent_to(path: String) {
        assertEquals("/consumers", path)
        assertEquals(
            "Expected the backend not to be called but got " +
                "${world.postInvocations()} POST(s) to $path",
            0,
            world.postInvocations(),
        )
    }

    @Then("a POST is sent to {string} with first name {string} and last name {string}")
    fun post_sent_to_with(path: String, firstName: String, lastName: String) {
        assertEquals("/consumers", path)
        val captured = world.capturedPosts()
        assertEquals(
            "Expected exactly one POST to $path, got ${captured.size}",
            1,
            captured.size,
        )
        assertEquals(
            "Expected POST first name '$firstName' but got '${captured.first().firstName}'",
            firstName,
            captured.first().firstName,
        )
        assertEquals(
            "Expected POST last name '$lastName' but got '${captured.first().lastName}'",
            lastName,
            captured.first().lastName,
        )
    }

    @Then("only one POST is sent to {string}")
    fun only_one_post(path: String) {
        assertEquals("/consumers", path)
        assertEquals(
            "Expected exactly one POST to $path, got ${world.postInvocations()}",
            1,
            world.postInvocations(),
        )
    }

    @Then("the {string} button is enabled again")
    fun button_re_enabled(name: String) {
        assertEquals("Continuar", name)
        val state = world.lastState()
        assertFalse(
            "Expected the button to be re-enabled (loading = false) but loading was " +
                "${state.loading}",
            state.loading,
        )
    }

    @Then("the local session is cleared")
    fun local_session_is_cleared() {
        assertNull(
            "Expected the local session to be cleared but it was still present",
            world.sessionValue(),
        )
    }

    @Then("the app signals {string}")
    fun app_signals_navigate_to_home(event: String) {
        assertEquals(
            "Step 'the app signals {string}' is bound to 'Navigate to Home'. Adapt " +
                "the step or the feature if you need a new navigation event.",
            "Navigate to Home",
            event,
        )
        val events = world.emittedEvents()
        assertEquals(
            "Expected exactly one NavigateToHome event, got $events",
            listOf<CompleteProfileEvent>(CompleteProfileEvent.NavigateToHome),
            events,
        )
    }
}

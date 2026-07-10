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
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.User
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
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
            .onNodeWithText("Completa tu perfil")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Nombre")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Apellido")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Continuar")
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
            .onNodeWithText("Continuar")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Hola,")
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
            .onNodeWithText("Continuar")
            .performClick()

        composeTestRule
            .onNodeWithText("El nombre es obligatorio")
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
            .onNodeWithText("Continuar")
            .performClick()

        composeTestRule
            .onNodeWithText("El apellido es obligatorio")
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
            .onNodeWithText("Continuar")
            .performClick()

        composeTestRule
            .activityRule
            .scenario
            .recreate()

        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithText("Completa tu perfil")
            .assertCountEquals(0)

        composeTestRule
            .onNodeWithText("Hola,")
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

    @EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface AuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }
}

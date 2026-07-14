package com.loresuelvo.consumer.acceptance.auth

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.data.auth.SessionStoreModule
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.components.SingletonComponent
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@dagger.hilt.android.testing.UninstallModules(SessionStoreModule::class)
@RunWith(AndroidJUnit4::class)
class WelcomeScreenAcceptanceTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Resolved via `@EntryPoint` from the test application: this
     * returns THE SAME `@Singleton` instance that the activity's
     * `SessionViewModel` is observing. The `@Inject` field pattern
     * is unreliable for interface types in `@HiltAndroidTest` runs
     * (it sometimes binds to a different component scope than the
     * one used by Hilt's `@HiltViewModel` injection), so we resolve
     * the port directly from the production
     * `SingletonComponent` graph.
     */
    private val sessionStore: AuthSessionStore by lazy {
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext<Application>(),
            AuthSessionStoreEntryPoint::class.java,
        ).authSessionStore()
    }

    /**
     * Resolves a string from the activity so assertions match the
     * text produced by `stringResource` under the device locale (the
     * CI emulator boots en-US, local devices may be es-AR). See
     * AGENTS.md "Aceptación: Locale del CI".
     */
    private fun localizedString(@StringRes resourceId: Int): String =
        composeTestRule.activity.getString(resourceId)

    @Before
    fun setUp() {
        hiltRule.inject()
        sessionStore.clearSession()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    // Scenario: 01-CPI Mostrar nombre y branding de LoResuelvo
    @Test
    fun displays_loresuelvo_branding() {

        composeTestRule
            .onNodeWithText(localizedString(R.string.brand_name))
            .assertIsDisplayed()
    }

    // Scenario: Mostrar la propuesta de valor antes de la autenticación
    @Test
    fun displays_value_proposition() {

        composeTestRule
            .onNodeWithText(localizedString(R.string.welcome_badge_verified))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(localizedString(R.string.welcome_title))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(localizedString(R.string.welcome_step1_title))
            .assertIsDisplayed()
    }

    // Scenario: 02-CPI Mostrar botón de Registrarse
    @Test
    fun displays_register_button() {

        composeTestRule
            .onNodeWithText(localizedString(R.string.welcome_register))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    // Scenario: 03-CPI Mostrar acción de Iniciar Sesión
    @Test
    fun displays_login_action() {

        composeTestRule
            .onNodeWithText(localizedString(R.string.welcome_login))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    // Scenario: 04-CPI Mostrar opción de continuar con Google
    @Test
    fun displays_google_login_button() {

        composeTestRule
            .onNodeWithText(localizedString(R.string.welcome_google))
            .assertIsDisplayed()
            .assertHasClickAction()
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

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }
}

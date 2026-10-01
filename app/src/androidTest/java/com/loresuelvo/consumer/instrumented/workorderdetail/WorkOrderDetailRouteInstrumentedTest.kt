package com.loresuelvo.consumer.instrumented.workorderdetail

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import com.loresuelvo.consumer.data.auth.SessionStoreModule
import com.loresuelvo.consumer.di.RepositoryModule
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.UserRepository
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderReview
import com.loresuelvo.consumer.instrumented.diagnosis.FakeDiagnosisRepository
import com.loresuelvo.consumer.instrumented.support.WirePinHarness
import com.loresuelvo.consumer.testdi.FakeConversationRepository
import com.loresuelvo.consumer.testdi.FakeJobRequestRepository
import com.loresuelvo.consumer.testdi.FakeServiceProposalRepository
import com.loresuelvo.consumer.testdi.FakeTurnosRepository
import com.loresuelvo.consumer.testdi.FakeWorkOrderDetailRepository
import com.loresuelvo.consumer.ui.navigation.WorkOrderDetailRoute
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_RATE_CANCEL_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_RATE_COMPOSER_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_RATE_CTA_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_RATE_STAR_TAG_PREFIX
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_RATE_SUBMIT_TAG
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration coverage for the US-30 `calify-provider-service`
 * **route wiring** — the contract that [WorkOrderDetailRoute]
 * (in `LoResuelvoNav.kt`) forwards every composer callback to
 * the Hilt-injected `WorkOrderDetailViewModel`.
 *
 * Why this test exists (post-mortem on commit `b1cc0eb`)
 *
 *  - The initial drop of US-30 left `WorkOrderDetailRoute`
 *    untouched on the composer-button change set, so every new
 *    callback (`onOpenReviewForm`, `onRatingChange`,
 *    `onDescriptionChange`, `onSubmitReview`,
 *    `onCancelReview`) defaulted to `{}` in production. The
 *    screen-level Compose UI tests still passed because they
 *    drive the screen with explicit lambdas; the JVM BDD suite
 *    passed because it talks to the VM through a hand-rolled
 *    `World` that bypasses the route entirely. The bug only
 *    surfaced in manual testing on a real device.
 *
 *  - This instrumented test pins the route→VM wire contract
 *    end-to-end: Hilt provides the same `@HiltViewModel`
 *    instance the production route resolves; the screen
 *    receives the production-route-issued callbacks (not the
 *    `{}` defaults); and [FakeWorkOrderDetailRepository]
 *    records the call the VM hands off. A future regression
 *    that omits one of the production wire callbacks from the
 *    route would fail this test with a clear assertion —
 *    preventing another silent regression.
 *
 * Setup mirrors the other acceptance suites in this repo:
 *
 *  - `@HiltAndroidTest` + `@UninstallModules(RepositoryModule::class,
 *    SessionStoreModule::class)` to install a deterministic graph.
 *  - `@EntryPoint` to resolve the same `@Singleton`
 *    [FakeWorkOrderDetailRepository] the route's Hilt VM
 *    observes, so [FakeWorkOrderDetailRepository.set] /
 *    [FakeWorkOrderDetailRepository.lastSubmission] /
 *    [FakeWorkOrderDetailRepository.enqueueSubmitOutcome] reach
 *    the same instance the production code sees.
 *  - `composeTestRule.activity.setContent { WorkOrderDetailRoute(...) }`
 *    mounts the production route directly inside the activity's
 *    Compose tree. `WorkOrderDetailRoute` is `internal` for
 *    exactly this test (no other consumer needs it).
 *
 * The CI emulator boots as `en-US` while developer devices may
 * use Spanish. Render assertions target `testTag`s — locale
 * independent — so the test stays green regardless of the
 * emulator locale.
 */
@HiltAndroidTest
@UninstallModules(RepositoryModule::class, SessionStoreModule::class)
@RunWith(AndroidJUnit4::class)
class WorkOrderDetailRouteInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val sessionStore: AuthSessionStore by lazy {
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext<Application>(),
            AuthSessionStoreEntryPoint::class.java,
        ).authSessionStore()
    }

    private val workOrderDetailRepo: FakeWorkOrderDetailRepository by lazy {
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext<Application>(),
            WorkOrderDetailRepositoryEntryPoint::class.java,
        ).workOrderDetailRepository()
    }

    private val provider = WorkOrderDetailCounterpart(
        id = "prov-99",
        name = "Andres",
        surname = "Colina",
        categoryName = "Plomería",
        profilePhotoUrl = null,
    )

    @Before
    fun setUp() {
        WirePinHarness.harnessSetup(hiltRule, composeTestRule)
        workOrderDetailRepo.set(paidWorkOrder())
        WirePinHarness.persistAuthSession(composeTestRule)
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    /**
     * The regression test for commit `b1cc0eb` — guarantees that
     * the production route wires `onOpenReviewForm = { viewModel.openReviewComposer() }`
     * so tapping "Calificar servicio" actually mutates the VM
     * state. Before this commit, the screen-local default `{}`
     * silently swallowed the tap.
     */
    @Test
    fun tapping_calificar_servicio_invokes_openReviewComposer_via_the_production_route() {
        composeTestRule.activity.setContent {
            WorkOrderDetailRoute(
                navController = androidx.navigation.compose.rememberNavController(),
                workOrderId = "wo-100",
                provider = provider,
            )
        }
        composeTestRule.waitForIdle()

        // The VM-side `load(...)` is a `viewModelScope.launch` that
        // runs on Dispatchers.Main. Poll with a deadline so the
        // assertion races against recomposition instead of failing
        // immediately on a fixed wait.
        WirePinHarness.waitForTag(composeTestRule, WORK_ORDER_RATE_CTA_TAG)
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_CTA_TAG).assertIsDisplayed()

        // The actual regression pin: this tap MUST reach the VM.
        // If the route's `onOpenReviewForm` defaults to `{}`, the
        // composer never expands and the next assertion fails
        // with "no nodes found", with a stack trace pointing at
        // `WorkOrderDetailRoute.onOpenReviewForm = { ... }` —
        // exactly the line that commit `b1cc0eb` fixed in
        // isolation by hand.
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_CTA_TAG).performClick()

        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_COMPOSER_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_SUBMIT_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_CANCEL_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(WORK_ORDER_RATE_CTA_TAG).assertCountEquals(0)
    }

    /**
     * Wire-pin for the submit path: the production route must
     * pass `onSubmitReview = { viewModel.submitReview() }` so the
     * typed call lands on the repository through the VM.
     * Catches the failure mode where one of the typed submit
     * callbacks was forgotten in the route, even when the
     * composer expansion alone still passed test 1.
     */
    @Test
    fun submit_reaches_the_repository_through_the_production_route() {
        composeTestRule.activity.setContent {
            WorkOrderDetailRoute(
                navController = androidx.navigation.compose.rememberNavController(),
                workOrderId = "wo-100",
                provider = provider,
            )
        }
        composeTestRule.waitForIdle()

        // Same poll pattern as the CTA test.
        WirePinHarness.waitForTag(composeTestRule, WORK_ORDER_RATE_CTA_TAG)
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_CTA_TAG).performClick()

        // Tap the 5-star button. The composer rating row carries
        // dedicated testTags; the row may sit below the viewport
        // on the small CI emulator, so scrollTo before clicking.
        composeTestRule
            .onNodeWithTag(WORK_ORDER_RATE_STAR_TAG_PREFIX + "5")
            .performScrollTo()
            .performClick()

        // Pin the happy-path success case so the composer
        // collapses (i.e., the submit fired).
        composeTestRule.runOnUiThread {
            workOrderDetailRepo.enqueueSubmitOutcome(
                SubmitWorkOrderReviewOutcome.Submitted(
                    WorkOrderReview(rating = 5, description = ""),
                ),
            )
        }
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_SUBMIT_TAG).performScrollTo()
            .performClick()

        // The crucial assertion: the route passed the typed call
        // through to the VM which forwarded to the fake repo. If
        // the route forgot `onSubmitReview = { viewModel.submitReview() }`,
        // the VM never fired and `lastSubmission` stays null.
        composeTestRule.waitForIdle()
        val recorded = workOrderDetailRepo.lastSubmission
        assertNotNull(
            "expected the route to hand the Enviar tap off to the VM " +
                "(which forwards to the repo). If the route forgot " +
                "`onSubmitReview`, this assertion catches it.",
            recorded,
        )
        assertEquals(
            "rating draft must reach the repo verbatim (1..5)",
            5,
            recorded!!.rating,
        )
        assertEquals(
            "workOrderId from the route argument must reach the repo",
            "wo-100",
            recorded.workOrderId,
        )
    }

    private fun paidWorkOrder(): WorkOrderDetail = WorkOrderDetail(
        proposalId = "wo-100",
        provider = provider,
        description = "Cambio de termotanque",
        amountCents = 8_500_000L,
        scheduledOnEpochMillis = 1_793_500_800_000L,
        acceptedOnEpochMillis = 1_788_434_364_640L,
        paidOnEpochMillis = 1_788_500_000_000L,
        status = TurnoStatus.Paid,
        completionReport = null,
        review = null,
        estimatedDurationMinutes = 90,
    )

    // ---------------------------------------------------------------
    // Route-specific bindings + EntryPoints. Most of the boilerplate
    // lives in [WirePinHarness] / its `testdi/` siblings; this
    // section is only what this test needs beyond the defaults.
    // ---------------------------------------------------------------

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class WorkOrderDetailRouteTestBindings {

        @Binds
        @Singleton
        abstract fun bindCalendarConnectionRepository(
            repository: com.loresuelvo.consumer.testdi.FakeCalendarConnectionRepository,
        ): com.loresuelvo.consumer.domain.calendar.CalendarConnectionRepository

        @Binds
        @Singleton
        abstract fun bindUserRepository(
            repository: WirePinHarness.SuccessfulFakeUserRepository,
        ): UserRepository

        @Binds
        @Singleton
        abstract fun bindAuthSessionStore(
            store: EncryptedAuthSessionStore,
        ): AuthSessionStore

        @Binds
        @Singleton
        abstract fun bindCategoryRepository(
            repository: WirePinHarness.StubCategoryRepository,
        ): CategoryRepository

        @Binds
        @Singleton
        abstract fun bindProviderRepository(
            repository: WirePinHarness.StubProviderRepository,
        ): ProviderRepository

        @Binds
        @Singleton
        abstract fun bindDiagnosisRepository(
            repository: FakeDiagnosisRepository,
        ): DiagnosisRepository

        @Binds
        @Singleton
        abstract fun bindJobRequestRepository(
            repository: FakeJobRequestRepository,
        ): JobRequestRepository

        @Binds
        @Singleton
        abstract fun bindConversationRepository(
            repository: FakeConversationRepository,
        ): ConversationRepository

        @Binds
        @Singleton
        abstract fun bindServiceProposalRepository(
            repository: FakeServiceProposalRepository,
        ): ServiceProposalRepository

        @Binds
        @Singleton
        abstract fun bindTurnosRepository(
            repository: FakeTurnosRepository,
        ): TurnosRepository

@Binds
        @Singleton
        abstract fun bindWorkOrderDetailRepository(
            repository: FakeWorkOrderDetailRepository,
        ): com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object WorkOrderDetailRouteTestSessionPrefsModule {
        @Provides
        @Singleton
        fun provideSessionPrefs(
            @ApplicationContext context: Context,
        ): SharedPreferences =
            context.getSharedPreferences(
                "auth_session_secure_wod_route_test",
                Context.MODE_PRIVATE,
            )
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkOrderDetailRepositoryEntryPoint {
        fun workOrderDetailRepository(): FakeWorkOrderDetailRepository
    }
}

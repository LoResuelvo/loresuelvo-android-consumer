package com.loresuelvo.consumer.ui.payment

import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.loresuelvo.consumer.BuildConfig
import com.loresuelvo.consumer.ui.navigation.Route
import com.loresuelvo.consumer.ui.screens.paymentresult.PaymentResultScreen
import com.loresuelvo.consumer.ui.screens.paymentresult.PaymentResultViewModel
import com.loresuelvo.consumer.ui.screens.serviceagreement.ServiceAgreementScreen
import com.loresuelvo.consumer.ui.screens.serviceagreement.ServiceAgreementViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Thin routes that bridge Compose Navigation with the US-21
 * (Confirm service agreement) and US-21/US-28 (post-redirect
 * payment result) flows.
 *
 * Each function:
 *  1. Resolves a Hilt-scoped VM via [hiltViewModel].
 *  2. Wires the VM events / polling into the host [NavHostController].
 *  3. Emits the screen state.
 *
 * The deep link is decoded in [decodePaymentIntentFromUri] and
 * parsed by Compose Navigation natively via the `deepLinks` list
 * declared in the `composable` entries.
 */
object ServiceAgreementRoute {

    /**
     * Renders the service-agreement screen and wires the
     * [ServiceAgreementViewModel] events into the host
     * [NavHostController]. The OpenCheckout event triggers a
     * Custom Tab + navigation to the payment-result route.
     */
    @Composable
    fun bind(
        navController: NavHostController,
        serviceAgreementViewModel: ServiceAgreementViewModel,
        onReturnHome: () -> Unit,
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val uiState by serviceAgreementViewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(serviceAgreementViewModel) {
            serviceAgreementViewModel.events.collectLatest { event ->
                when (event) {
                    is com.loresuelvo.consumer.ui.screens.serviceagreement.ServiceAgreementEvent.OpenCheckout -> {
                        openCheckoutCustomTab(context, event.checkoutUrl)
                    }
                    com.loresuelvo.consumer.ui.screens.serviceagreement.ServiceAgreementEvent.Cancelled -> {
                        navController.popBackStack()
                    }
                }
            }
        }

        ServiceAgreementScreen(
            state = uiState,
            onRetry = {
                // We don't carry the proposalId inside the VM; the
                // host re-loads it. In practice the proposalId is
                // only known when the host's `onProposalSelected`
                // route handler invokes `viewModel.load(id)`, so
                // this path is rarely exercised: errors happen at
                // the load step, not the confirm step.
                scope.launch { serviceAgreementViewModel.load(currentServiceProposalId(uiState)) }
            },
            onConfirm = serviceAgreementViewModel::confirmAgreement,
            onCancel = serviceAgreementViewModel::cancel,
            onReturnHome = onReturnHome,
            onPaidAlready = onReturnHome,
        )
    }

    private fun currentServiceProposalId(state: com.loresuelvo.consumer.ui.screens.serviceagreement.ServiceAgreementUiState): Int = state.serviceProposalId

    private fun openCheckoutCustomTab(context: android.content.Context, url: String) {
        try {
            val customTabs = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabs.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            // Some emulators / browsers fail to resolve the custom-tabs intent.
            // Fall back to a regular browser intent so the user never gets stuck.
            Log.w("ServiceAgreementRoute", "Custom Tabs unavailable, falling back to VIEW", e)
            val fallback = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallback)
        }
    }
}

object PaymentResultRoute {

    /**
     * Renders the payment-result screen and starts the polling
     * loop on first composition. The route is reached either from
     * the App Link (deep link) or from the in-app flow after
     * [ServiceAgreementRoute] opens the Custom Tab.
     */
    @Composable
    fun bind(
        navController: NavHostController,
        backStackEntry: NavBackStackEntry,
        onReturnHome: () -> Unit,
    ) {
        val paymentIntentId = backStackEntry.arguments
            ?.getString(Route.ARG_EXTERNAL_REFERENCE)
            .orEmpty()
        val viewModel: PaymentResultViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(paymentIntentId) {
            if (paymentIntentId.isNotBlank()) viewModel.startPolling(paymentIntentId)
        }

        PaymentResultScreen(
            state = state,
            onRetry = {
                viewModel.startPolling(paymentIntentId)
            },
            onReturnHome = {
                navController.popBackStack(Route.Home.path, inclusive = false)
                onReturnHome()
            },
        )
    }
}

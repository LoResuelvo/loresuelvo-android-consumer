package com.loresuelvo.consumer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Pure graph layer. The host ([com.loresuelvo.consumer.ui.navigation.LoResuelvoNav])
 * owns the `Scaffold` slot (bottom navigation bar) and the
 * smart-router logic; this composable only declares the routes and
 * the screen-typed Composable for each one. The two layers are
 * split so the host can be unit-tested in isolation — the graph
 * is a pure consumer of the screen composables.
 *
 * `contentPadding` carries the Scaffold insets (top status bar +
 * bottom nav bar when visible). The graph wraps the [NavHost] in
 * a [Box] with that padding so the bottom bar never overlaps the
 * scrollable content of any screen.
 */
@Composable
fun LoResuelvoNavHost(
    navController: NavHostController,
    startDestination: String,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    welcome: @Composable () -> Unit,
    completeProfile: @Composable () -> Unit,
    home: @Composable () -> Unit,
    categories: @Composable () -> Unit,
    professionals: @Composable (categoryId: Int, categoryName: String) -> Unit,
    chat: @Composable (conversationId: String?) -> Unit,
    conversation: @Composable (conversationId: String) -> Unit,
    messages: @Composable () -> Unit,
    assistant: @Composable () -> Unit,
    misServicios: @Composable () -> Unit,
    turnos: @Composable () -> Unit = {},
    workOrderDetail: @Composable (workOrderId: String, provider: com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart?) -> Unit,
    serviceAgreement: @Composable (NavHostController) -> Unit = {},
    paymentResult: @Composable (NavHostController, androidx.navigation.NavBackStackEntry) -> Unit = { _, _ -> },
) {
    Box(modifier = Modifier.padding(contentPadding)) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(Route.Welcome.path) { welcome() }
            composable(Route.CompleteProfile.path) { completeProfile() }
            composable(Route.Home.path) { home() }
            // 02-UXUI: dedicated screen for every category published
            // by the platform, reachable from the Home "Ver todas"
            // link. Hidden from the bottom nav.
            composable(Route.Categories.path) { categories() }
            composable(
                route = Route.Professionals(
                    categoryId = -1,
                    categoryName = "_ignored_",
                ).path,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.IntType },
                    navArgument("categoryName") { type = NavType.StringType },
                ),
            ) { entry ->
                val categoryId = entry.arguments?.getInt("categoryId") ?: -1
                val categoryName = entry.arguments?.getString("categoryName").orEmpty()
                professionals(categoryId, categoryName)
            }
            composable(
                route = Route.Chat(conversationId = null).path,
                arguments = listOf(
                    navArgument("conversationId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val conversationId = entry.arguments?.getString("conversationId")
                chat(conversationId)
            }
            composable(
                route = Route.Conversation(conversationId = "_ignored_").path,
                arguments = listOf(
                    navArgument("conversationId") { type = NavType.StringType },
                ),
            ) { entry ->
                val conversationId = entry.arguments?.getString("conversationId").orEmpty()
                conversation(conversationId)
            }
            // Bottom-bar destinations (US-18).
            composable(Route.Messages.path) { messages() }
            composable(Route.Assistant.path) { assistant() }
            // US-54 scenario 03-VSP: every proposal regardless of
            // status, reached from the Home "Ver todas" link.
            composable(Route.MisServicios.path) { misServicios() }
            // visualize-turns.feature scenario 01-VT: dedicated
            // Mis Turnos screen, reached from the Home "Ver
            // todas" link.
            composable(Route.Turnos.path) { turnos() }
            // US-54 scenario 16-VSP + US-56 (`visualize-turns-detail`):
            // work-order detail. Reached from the "Ver orden de
            // trabajo" CTA on [ProposalDetailScreen] (US-54) and
            // from the "Mis Turnos" list / Home preview card
            // (US-56). The arg is the dedicated work-order id
            // since the backend `GET /work-orders/{id}` endpoint
            // exists; the upstream US-54 code that surfaced this
            // route keyed on the proposal id is being migrated
            // commit by commit.
            composable(
                route = Route.WorkOrderDetail.path,
                arguments = listOf(
                    androidx.navigation.navArgument(Route.WorkOrderDetail.ARG_WORK_ORDER_ID) {
                        type = androidx.navigation.NavType.StringType
                    },
                    androidx.navigation.navArgument(Route.WorkOrderDetail.ARG_PROVIDER_ID) {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    androidx.navigation.navArgument(Route.WorkOrderDetail.ARG_PROVIDER_NAME) {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    androidx.navigation.navArgument(Route.WorkOrderDetail.ARG_PROVIDER_SURNAME) {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    androidx.navigation.navArgument(Route.WorkOrderDetail.ARG_PROVIDER_CATEGORY_NAME) {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    androidx.navigation.navArgument(Route.WorkOrderDetail.ARG_PROVIDER_PROFILE_PHOTO_URL) {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val workOrderId = entry.arguments
                    ?.getString(Route.WorkOrderDetail.ARG_WORK_ORDER_ID).orEmpty()
                val provider = entry.arguments?.getString(Route.WorkOrderDetail.ARG_PROVIDER_ID)?.takeIf { it.isNotBlank() }?.let { id ->
                    com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart(
                        id = id,
                        name = entry.arguments?.getString(Route.WorkOrderDetail.ARG_PROVIDER_NAME).orEmpty(),
                        surname = entry.arguments?.getString(Route.WorkOrderDetail.ARG_PROVIDER_SURNAME).orEmpty(),
                        categoryName = entry.arguments?.getString(Route.WorkOrderDetail.ARG_PROVIDER_CATEGORY_NAME).orEmpty(),
                        profilePhotoUrl = entry.arguments?.getString(Route.WorkOrderDetail.ARG_PROVIDER_PROFILE_PHOTO_URL),
                    )
                }
                workOrderDetail(workOrderId, provider)
            }

            // US-21: Service-agreement confirmation. The host is
            // responsible for starting the VM's `load(...)` from
            // the previous screen (the proposal detail bottom
            // sheet). The route has no args: the VM is keyed on a
            // single in-flight proposal.
            composable(Route.ServiceAgreement.path) {
                serviceAgreement(navController)
            }

            // US-21 / US-28: post-redirect payment result.
            //
            // Mercado Pago returns the consumer to one of the public payment
            // result URLs. The payment intent is correlated through the
            // `external_reference` query parameter. The actual payment status
            // is resolved from the backend, not from the redirect parameters.
            composable(
                route = Route.PaymentResult.path,
                arguments = listOf(
                    navArgument(Route.ARG_EXTERNAL_REFERENCE) {
                        type = NavType.StringType
                    }
                ),
            ) { entry ->
                paymentResult(navController, entry)
            }
        }
    }
}

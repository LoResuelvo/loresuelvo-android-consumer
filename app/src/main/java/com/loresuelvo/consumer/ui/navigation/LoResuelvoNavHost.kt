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
    content: ConsumerNavContent,
) {
    Box(modifier = Modifier.padding(contentPadding)) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(Route.Welcome.path) { content.session.welcome() }
            composable(Route.CompleteProfile.path) { content.session.completeProfile() }
            composable(Route.Home.path) { content.discovery.home() }
            composable(Route.MyProfile.path) { content.account.myProfile() }
            // by the platform, reachable from the Home "Ver todas"
            // link. Hidden from the bottom nav.
            composable(Route.Categories.path) { content.discovery.categories() }
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
                content.discovery.professionals(categoryId, categoryName)
            }
            composable(
                route = Route.ProviderProfile(providerId = -1).path,
                arguments = listOf(
                    navArgument("providerId") { type = NavType.IntType },
                ),
            ) { entry ->
                val providerId = entry.arguments?.getInt("providerId") ?: -1
                content.discovery.providerProfile(providerId)
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
                content.chat.chat(conversationId)
            }
            composable(
                route = Route.Conversation(conversationId = "_ignored_").path,
                arguments = listOf(
                    navArgument("conversationId") { type = NavType.StringType },
                ),
            ) { entry ->
                val conversationId = entry.arguments?.getString("conversationId").orEmpty()
                content.chat.conversation(conversationId)
            }
            composable(Route.Messages.path) { content.chat.messages() }
            composable(Route.Assistant.path) { content.chat.assistant() }
            // status, reached from the Home "Ver todas" link.
            composable(Route.MisServicios.path) { content.work.misServicios(null) }
            composable(
                route = Route.MisServiciosProposal.path,
                arguments = listOf(
                    navArgument(Route.MisServiciosProposal.ARG_PROPOSAL_ID) {
                        type = NavType.StringType
                    },
                ),
            ) { entry ->
                val proposalId = entry.arguments
                    ?.getString(Route.MisServiciosProposal.ARG_PROPOSAL_ID)
                    .orEmpty()
                content.work.misServicios(proposalId)
            }
            // Mis Turnos screen, reached from the Home "Ver
            // todas" link.
            composable(Route.Turnos.path) { content.work.turnos() }
            // Work-order detail, reached from the "Mis Turnos" list
            // or the Home preview card. The route keeps the proposal
            // id as its stable navigation key.
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
                content.work.workOrderDetail(workOrderId, provider)
            }

            // responsible for starting the VM's `load(...)` from
            // the previous screen (the proposal detail bottom
            // sheet). The route has no args: the VM is keyed on a
            // single in-flight proposal.
            composable(Route.ServiceAgreement.path) {
                content.payment.serviceAgreement(navController)
            }

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
                content.payment.paymentResult(navController, entry)
            }
        }
    }
}

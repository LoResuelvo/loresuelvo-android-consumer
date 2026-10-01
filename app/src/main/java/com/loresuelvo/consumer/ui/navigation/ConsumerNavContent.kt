package com.loresuelvo.consumer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart

/**
 * Feature-sized content contracts consumed by the declarative graph.
 * Keeping these groups typed prevents a new launcher or route argument
 * from expanding one positional parameter list in [LoResuelvoNavHost].
 */
data class ConsumerNavContent(
    val session: SessionNavContent,
    val account: AccountNavContent,
    val discovery: DiscoveryNavContent,
    val chat: ChatNavContent,
    val work: WorkNavContent,
    val payment: PaymentNavContent,
)

data class SessionNavContent(
    val welcome: @Composable () -> Unit,
    val completeProfile: @Composable () -> Unit,
)

data class AccountNavContent(
    val myProfile: @Composable () -> Unit,
)

data class DiscoveryNavContent(
    val home: @Composable () -> Unit,
    val categories: @Composable () -> Unit,
    val professionals: @Composable (categoryId: Int, categoryName: String) -> Unit,
    val providerProfile: @Composable (providerId: Int) -> Unit,
)

data class ChatNavContent(
    val chat: @Composable (conversationId: String?) -> Unit,
    val conversation: @Composable (conversationId: String) -> Unit,
    val messages: @Composable () -> Unit,
    val assistant: @Composable () -> Unit,
)

data class WorkNavContent(
    val misServicios: @Composable () -> Unit,
    val turnos: @Composable () -> Unit,
    val workOrderDetail: @Composable (
        workOrderId: String,
        provider: WorkOrderDetailCounterpart?,
    ) -> Unit,
)

data class PaymentNavContent(
    val serviceAgreement: @Composable (NavHostController) -> Unit,
    val paymentResult: @Composable (NavHostController, NavBackStackEntry) -> Unit,
)

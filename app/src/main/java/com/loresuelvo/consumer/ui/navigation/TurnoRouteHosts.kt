package com.loresuelvo.consumer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.ui.screens.turnos.TurnosScreen
import com.loresuelvo.consumer.ui.screens.turnos.TurnosViewModel

/** Route hosts for consumer appointments and work-order detail. */

@Composable
internal fun TurnosRoute(
    navController: NavHostController,
) {
    val viewModel: TurnosViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()

    TurnosScreen(
        state = state,
        onRetryClick = viewModel::load,
        onTurnoCardClick = { turnoId ->
            val turno = (state as? com.loresuelvo.consumer.ui.screens.turnos.TurnosUiState.Ready)
                ?.turnos
                ?.firstOrNull { it.id == turnoId }
            val provider = turno?.counterpart?.let { counterpart ->
                WorkOrderDetailCounterpart(
                    id = counterpart.id,
                    name = counterpart.name,
                    surname = counterpart.surname,
                    categoryName = counterpart.categoryName,
                    profilePhotoUrl = counterpart.profilePhotoUrl,
                )
            }
            navController.navigate(Route.WorkOrderDetail.buildPath(turnoId, provider))
        },
    )
}

@Composable
internal fun WorkOrderDetailRoute(
    navController: NavHostController,
    workOrderId: String,
    provider: WorkOrderDetailCounterpart?,
) {
    val viewModel: com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailViewModel =
        hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(workOrderId, provider) {
        viewModel.load(workOrderId, provider)
    }
    LaunchedEffect(viewModel) {
        viewModel.checkoutUrl.collect { url ->
            androidx.browser.customtabs.CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, android.net.Uri.parse(url))
        }
    }

    com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailScreen(
        state = state,
        onRetry = { viewModel.load(workOrderId, provider) },
        onBackClick = { navController.popBackStack() },
        onPayNow = { viewModel.payNow(workOrderId) },
        onOpenReviewForm = { viewModel.openReviewComposer() },
        onRatingChange = viewModel::onRatingChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSubmitReview = viewModel::submitReview,
        onCancelReview = viewModel::cancelReviewComposer,
    )
}

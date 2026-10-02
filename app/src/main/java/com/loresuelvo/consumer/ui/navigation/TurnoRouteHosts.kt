package com.loresuelvo.consumer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.ui.screens.turnos.TurnosScreen
import com.loresuelvo.consumer.ui.screens.turnos.TurnosViewModel

/** Route hosts for consumer appointments and work-order detail. */

@Composable
internal fun TurnosRoute(
    navController: NavHostController,
) {
    val viewModel: TurnosViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    TurnosScreen(
        state = state,
        onRetryClick = viewModel::load,
        onCalendarConnectionClick = {
            navController.navigate(Route.MyProfile.path) { launchSingleTop = true }
        },
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
        actions = com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailActions(
            onRetry = { viewModel.load(workOrderId, provider) },
            onPayNow = { viewModel.payNow(workOrderId) },
            review = com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailActions.ReviewActions(
                onOpen = viewModel::openReviewComposer,
                onRatingChange = viewModel::onRatingChange,
                onDescriptionChange = viewModel::onDescriptionChange,
                onSubmit = viewModel::submitReview,
                onCancel = viewModel::cancelReviewComposer,
            ),
        ),
    )
}

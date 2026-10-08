package com.loresuelvo.consumer.ui.navigation

import androidx.navigation.NavHostController
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import com.loresuelvo.consumer.platform.notifications.ServiceTarget

internal object NotificationRouteMapper {
    fun message(conversationId: Int): String =
        Route.Conversation.buildPath(conversationId.toString())

    fun service(target: ServiceTarget): String? = when (target.destination) {
        AuthorizeServiceNotificationUseCase.DESTINATION_PROPOSAL ->
            Route.MisServiciosProposal.buildPath(target.resourceId)
        AuthorizeServiceNotificationUseCase.DESTINATION_WORK_ORDER ->
            Route.WorkOrderDetail.buildPath(target.resourceId)
        else -> null
    }
}

internal fun NavHostController.navigateFromNotification(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

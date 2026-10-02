package com.loresuelvo.consumer.ui.navigation

import android.net.Uri

sealed class Route(val path: String) {
    data object Welcome : Route("welcome")
    data object CompleteProfile : Route("complete_profile")
    data object Home : Route("home")
    data object MyProfile : Route("my-profile")


    data object Categories : Route("categories")

    /**
     * AI diagnostic chat screen. Reached from the `AiSearchBar` on
     * Home (the "Chat con IA" entry point, fresh conversation) and
     * from the bottom-bar "Asistente IA" tab → AssistantScreen →
     * session detail (resume). The optional [conversationId] arg
     * threads the resume flow: when present, the route hands it
     * to the chat VM, which loads the saved conversation via
     * `GET /chatbot/conversations/{id}` and hydrates the scroll
     * before the user can type.
     */
    data class Chat(val conversationId: String? = null) :
        Route("chat?conversationId={conversationId}") {
        companion object {
            fun buildPath(conversationId: String? = null): String =
                if (conversationId.isNullOrBlank()) "chat"
                else "chat?conversationId=$conversationId"
        }
    }

    /**
     * Provider conversation (1:1 chat between the consumer and
     * the provider, created by `POST /job-requests`). The host
     * composable is a placeholder for now — the actual message
     * UI is fleshed out in a follow-up US (scenarios 03-SRP and
     * 04-SRP of `contact-provider.feature`).
     */
    data class Conversation(val conversationId: String) :
        Route("conversation/{conversationId}") {
        companion object {
            fun buildPath(conversationId: String): String =
                "conversation/$conversationId"
        }
    }

    /**
     * Provider list for a single category. The category name is
     * display-only (rendered in the header) — the underlying query
     * is always by `categoryId`. It is URL-encoded in the path so
     * accents (`Plomería`) survive navigation round-trips.
     */
    data class Professionals(
        val categoryId: Int,
        val categoryName: String,
    ) : Route("professionals/{categoryId}/{categoryName}") {
        companion object {
            fun buildPath(categoryId: Int, categoryName: String): String =
                "professionals/$categoryId/" +
                    Uri.encode(categoryName)
        }
    }

    /** Public provider profile with reputation and completed work history. */
    data class ProviderProfile(val providerId: Int) :
        Route("provider-profile/{providerId}") {
        companion object {
            fun buildPath(providerId: Int): String = "provider-profile/$providerId"
        }
    }

    //
    // The path strings here are duplicated in
    // `BottomDestination.Companion` (intentionally, to keep the
    // bottom-nav component decoupled from the navigation graph).
    // Keep both in sync when renaming a route.

    /**
     * Messages list (consumer's conversations with providers).
     * Reachable from the bottom-bar "Mensajes" tab. The actual
     * list of conversations is fleshed out in scenario 03-SRP of
     * the messaging BDD; this commit only registers the route.
     */
    data object Messages : Route("messages")

    /**
     * AI assistant landing screen (sessions list + "nueva
     * conversación" entry point). Reachable from the bottom-bar
     * "Asistente IA" tab. Detail / per-session screens are
     * `Route.Chat` (existing) once a session is opened.
     */
    data object Assistant : Route("assistant")


    data object MisServicios : Route("mis-servicios")


    data object Turnos : Route("turnos")

    /**
     * Work-order detail screen. The route keeps the dedicated
     * work-order id and, when the consumer enters from a list
     * already loaded by `GET /work-orders`, also carries the
     * provider metadata the detail endpoint does not repeat.
     *
     * The provider block is encoded as optional query params, not
     * added to the backend contract, so the app can reuse the
     * `counterpart` already on the list screen and avoid a second
     * fetch just to render the header.
     */
    data object WorkOrderDetail : Route("work-order-detail/{workOrderId}?providerId={providerId}&providerName={providerName}&providerSurname={providerSurname}&providerCategoryName={providerCategoryName}&providerProfilePhotoUrl={providerProfilePhotoUrl}") {
        const val ARG_WORK_ORDER_ID: String = "workOrderId"
        const val ARG_PROVIDER_ID: String = "providerId"
        const val ARG_PROVIDER_NAME: String = "providerName"
        const val ARG_PROVIDER_SURNAME: String = "providerSurname"
        const val ARG_PROVIDER_CATEGORY_NAME: String = "providerCategoryName"
        const val ARG_PROVIDER_PROFILE_PHOTO_URL: String = "providerProfilePhotoUrl"

        fun buildPath(
            workOrderId: String,
            provider: com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart? = null,
        ): String = buildString {
            append("work-order-detail/$workOrderId")
            if (provider == null) return@buildString
            append("?providerId=${Uri.encode(provider.id)}")
            append("&providerName=${Uri.encode(provider.name)}")
            append("&providerSurname=${Uri.encode(provider.surname)}")
            append("&providerCategoryName=${Uri.encode(provider.categoryName)}")
            append("&providerProfilePhotoUrl=${Uri.encode(provider.profilePhotoUrl.orEmpty())}")
        }
    }

    data object ServiceAgreement : Route("service-agreement")


    data object PaymentResult : Route(
        "payment-result?external_reference={external_reference}"
    ) {
        fun buildPath(externalReference: String): String =
            "payment-result?external_reference=${Uri.encode(externalReference)}"
    }

    companion object {
        const val PAYMENT_RETURN_SUCCESS_PATH = "/payments/success"
        const val PAYMENT_RETURN_PENDING_PATH = "/payments/pending"
        const val PAYMENT_RETURN_FAILURE_PATH = "/payments/failure"

        const val ARG_EXTERNAL_REFERENCE = "external_reference"
    }
}

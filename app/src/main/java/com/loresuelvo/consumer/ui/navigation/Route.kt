package com.loresuelvo.consumer.ui.navigation

import android.net.Uri

sealed class Route(val path: String) {
    data object Welcome : Route("welcome")
    data object CompleteProfile : Route("complete_profile")
    data object Home : Route("home")

    /**
     * Dedicated screen that lists every service category
     * available on the platform. Reached from the "Ver todas"
     * link on the Home category section (scenario 02-UXUI).
     * Each tile opens the existing
     * [Professionals] route when tapped.
     */
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

    // ---- Bottom-bar destinations (US-18) ----------------------
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

    /**
     * "Mis Servicios" surface — the consumer-facing list of
     * every service proposal regardless of status (US-54 scenario
     * 03-VSP). Reached from the Home "Ver todas" link in the
     * dedicated Mis Servicios section. Hidden from the bottom
     * nav (it's a sub-section of Home, not a top-level tab).
     */
    data object MisServicios : Route("mis-servicios")
}

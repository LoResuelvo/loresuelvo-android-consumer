package com.loresuelvo.consumer.data.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Debug-only OkHttp interceptor that short-circuits
 * `GET /work-orders` with a hand-crafted JSON payload so the
 * "Mis Turnos" surface can be exercised manually on a real
 * device or emulator without a live backend.
 *
 * **Source set**: `src/debug/` — the file is compiled into every
 * `*Debug` build (devDebug, stagingDebug, prodDebug) but is
 * **absent** from `*Release`. This is the Android-standard
 * convention for debug-only behaviour and avoids the
 * `DuplicateBindings` tax that a Hilt-flavor override would
 * impose.
 *
 * **Wiring**: `NetworkModule.provideOkHttpClient` checks
 * `BuildConfig.DEBUG` and adds this interceptor only when running
 * a debug build. Returning mocked data from a release APK would
 * be a security regression.
 *
 * **Manual testing flow** (visualize-turns.feature scenarios
 * 02-VT..09-VT):
 *  1. Build & install `devDebug` APK on a device / emulator.
 *  2. Launch the app, sign in (Auth0 flow, see README).
 *  3. Tap "Mis Turnos" on the Home dashboard.
 *  4. The four seeded turnos land in the list with one of each
 *     `TurnoStatus` (Pending / Confirmed / Finished / Cancelled)
 *     so every status-badge branch is reachable.
 */
class FakeTurnosInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Only intercept `GET /work-orders` (the list endpoint
        // Mis Turnos renders). All other requests pass through.
        if (request.method != "GET" || !path.endsWith("/work-orders")) {
            return chain.proceed(request)
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(MOCK_WORK_ORDERS_JSON.toResponseBody(JSON_MEDIA_TYPE))
            .addHeader("Content-Type", "application/json")
            .build()
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /**
         * Four representative turnos covering the per-scenario
         * surface the BDD + instrumented suites pin. Statuses
         * mirror what the dev wants to click through manually:
         *  - id 1: Confirmed  → badge "Confirmado"  (07-VT)
         *  - id 2: Pending    → badge "Pendiente"    (06-VT)
         *  - id 3: Finished   → badge "Finalizado"   (08-VT)
         *  - id 4: Cancelled  → badge "Cancelado"    (09-VT)
         *
         * Wire shape validated against the dev backend's
         * `GET /work-orders` response (2026-09):
         * snake_case, ISO-8601 with trailing `Z`, status `"scheduled"`
         * mapped to `TurnoStatus.Confirmed` by the mapper.
         */
        val MOCK_WORK_ORDERS_JSON: String = """
            [
              {
                "id": 1,
                "service_proposal_id": 101,
                "amount_cents": 1500050,
                "scheduled_on": "2026-10-15T14:30:00Z",
                "description": "Reparación de pérdida de agua en cocina",
                "status": "scheduled",
                "accepted_on": "2026-10-10T09:00:00Z",
                "counterpart": {
                  "id": 10,
                  "role": "provider",
                  "name": "Juan",
                  "surname": "Gómez",
                  "category_name": "Plomería",
                  "profile_photo_url": null
                }
              },
              {
                "id": 2,
                "service_proposal_id": 102,
                "amount_cents": 850000,
                "scheduled_on": "2026-10-20T10:00:00Z",
                "description": "Cambio de disyuntor",
                "status": "scheduled",
                "accepted_on": "2026-10-12T15:00:00Z",
                "counterpart": {
                  "id": 11,
                  "role": "provider",
                  "name": "Ana",
                  "surname": "Pérez",
                  "category_name": "Electricidad",
                  "profile_photo_url": null
                }
              },
              {
                "id": 3,
                "service_proposal_id": 103,
                "amount_cents": 2200000,
                "scheduled_on": "2026-09-30T16:00:00Z",
                "description": "Pintura de living y comedor",
                "status": "scheduled",
                "accepted_on": "2026-09-25T11:00:00Z",
                "counterpart": {
                  "id": 12,
                  "role": "provider",
                  "name": "Luis",
                  "surname": "Suárez",
                  "category_name": "Pintura",
                  "profile_photo_url": null
                }
              },
              {
                "id": 4,
                "service_proposal_id": 104,
                "amount_cents": 600000,
                "scheduled_on": "2026-10-25T09:00:00Z",
                "description": "Revisión de calefón (cancelado por el cliente)",
                "status": "scheduled",
                "accepted_on": "2026-10-15T13:00:00Z",
                "counterpart": {
                  "id": 13,
                  "role": "provider",
                  "name": "Marta",
                  "surname": "García",
                  "category_name": "Gas",
                  "profile_photo_url": null
                }
              }
            ]
        """.trimIndent()
    }
}

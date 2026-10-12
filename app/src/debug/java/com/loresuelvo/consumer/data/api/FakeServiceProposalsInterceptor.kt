package com.loresuelvo.consumer.data.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class FakeServiceProposalsInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (request.method != "GET" || path != "/service-proposals") {
            return chain.proceed(request)
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(MOCK_SERVICE_PROPOSALS_JSON.toResponseBody(JSON_MEDIA_TYPE))
            .addHeader("Content-Type", "application/json")
            .build()
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val MOCK_SERVICE_PROPOSALS_JSON: String = """
            [
              {
                "id": 201,
                "conversation_id": 301,
                "amount_cents": 1250000,
                "scheduled_on": "2026-10-18T14:30:00Z",
                "description": "Reparación de pérdida de agua en cocina",
                "status": "pending",
                "created_on": "2026-10-12T09:30:00Z",
                "estimated_duration_minutes": 90,
                "counterpart": {
                  "id": 21,
                  "role": "provider",
                  "name": "Juan",
                  "surname": "Gómez",
                  "category_name": "Plomería",
                  "profile_photo_url": null,
                  "identity_verified": true
                }
              },
              {
                "id": 202,
                "conversation_id": 302,
                "amount_cents": 890000,
                "scheduled_on": "2026-10-22T10:00:00Z",
                "description": "Instalación de luminarias en el living",
                "status": "accepted",
                "created_on": "2026-10-10T15:00:00Z",
                "estimated_duration_minutes": 120,
                "counterpart": {
                  "id": 22,
                  "role": "provider",
                  "name": "Ana",
                  "surname": "Pérez",
                  "category_name": "Electricidad",
                  "profile_photo_url": null,
                  "identity_verified": false
                }
              },
              {
                "id": 203,
                "conversation_id": 303,
                "amount_cents": 2100000,
                "scheduled_on": "2026-11-02T09:00:00Z",
                "description": "Pintura de living y comedor",
                "status": "rejected",
                "created_on": "2026-10-08T11:45:00Z",
                "estimated_duration_minutes": 480,
                "counterpart": {
                  "id": 23,
                  "role": "provider",
                  "name": "Luis",
                  "surname": "Suárez",
                  "category_name": "Pintura",
                  "profile_photo_url": null,
                  "identity_verified": true
                }
              },
              {
                "id": 204,
                "conversation_id": 304,
                "amount_cents": 650000,
                "scheduled_on": "2026-11-08T16:00:00Z",
                "description": "Revisión y mantenimiento del calefón",
                "status": "pending",
                "created_on": "2026-10-05T08:20:00Z",
                "estimated_duration_minutes": 60,
                "counterpart": {
                  "id": 24,
                  "role": "provider",
                  "name": "Marta",
                  "surname": "García",
                  "category_name": "Gas",
                  "profile_photo_url": null,
                  "identity_verified": false
                }
              }
            ]
        """.trimIndent()
    }
}

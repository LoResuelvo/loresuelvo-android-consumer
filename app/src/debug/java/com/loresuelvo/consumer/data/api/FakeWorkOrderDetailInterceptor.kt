package com.loresuelvo.consumer.data.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class FakeWorkOrderDetailInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (request.method != "GET" || !path.matches(WORK_ORDER_DETAIL_PATH)) {
            return chain.proceed(request)
        }

        val workOrderId = path.substringAfterLast('/').toLong()
        val body = when (workOrderId) {
            5L -> awaitingPaymentJson(workOrderId)
            6L -> paidJson(workOrderId)
            7L -> paidWithoutReviewJson(workOrderId)
            else -> scheduledJson(workOrderId)
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody(JSON_MEDIA_TYPE))
            .addHeader("Content-Type", "application/json")
            .build()
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val WORK_ORDER_DETAIL_PATH = Regex(".*/work-orders/\\d+")

        fun scheduledJson(workOrderId: Long): String = """
            {
              "id": $workOrderId,
              "service_proposal_id": ${100 + workOrderId},
              "consumer_id": 1,
              "provider_id": ${10 + workOrderId},
              "amount_cents": 1500050,
              "scheduled_on": "2026-10-15T14:30:00Z",
              "description": "Reparación de pérdida de agua en cocina",
              "status": "scheduled",
              "accepted_on": "2026-10-10T09:00:00Z",
              "paid_on": null,
              "completion_report": null,
              "review": null
            }
        """.trimIndent()

        fun awaitingPaymentJson(workOrderId: Long): String = """
            {
              "id": $workOrderId,
              "service_proposal_id": ${100 + workOrderId},
              "consumer_id": 1,
              "provider_id": ${10 + workOrderId},
              "amount_cents": 1800000,
              "scheduled_on": "2026-10-18T09:00:00Z",
              "description": "Instalación de aire acondicionado split",
              "status": "awaiting_payment",
              "accepted_on": "2026-10-14T11:00:00Z",
              "paid_on": null,
              "completion_report": null,
              "review": null
            }
        """.trimIndent()

        fun paidJson(workOrderId: Long): String = """
            {
              "id": $workOrderId,
              "service_proposal_id": ${100 + workOrderId},
              "consumer_id": 1,
              "provider_id": ${10 + workOrderId},
              "amount_cents": 950000,
              "scheduled_on": "2026-10-05T13:00:00Z",
              "description": "Mantenimiento preventivo de calefón",
              "status": "paid",
              "accepted_on": "2026-09-28T10:30:00Z",
              "paid_on": "2026-10-05T16:45:00Z",
              "completion_report": {
                "id": 6001,
                "description": "Trabajo finalizado y equipo probado correctamente.",
                "reported_on": "2026-10-05T16:00:00Z",
                "images": []
              },
              "review": {
                "rating": 5,
                "description": "Excelente atención y trabajo prolijo."
              }
            }
        """.trimIndent()

        fun paidWithoutReviewJson(workOrderId: Long): String = """
            {
              "id": $workOrderId,
              "service_proposal_id": ${100 + workOrderId},
              "consumer_id": 1,
              "provider_id": ${10 + workOrderId},
              "amount_cents": 1250000,
              "scheduled_on": "2026-10-10T11:00:00Z",
              "description": "Reparación de instalación eléctrica finalizada, pendiente de reseña",
              "status": "paid",
              "accepted_on": "2026-10-01T10:30:00Z",
              "paid_on": "2026-10-10T14:20:00Z",
              "completion_report": {
                "id": 7001,
                "description": "Trabajo finalizado y probado correctamente.",
                "reported_on": "2026-10-10T13:45:00Z",
                "images": [
                  {
                    "file_id": "evidence-7-1",
                    "original_name": "tablero-antes.jpg",
                    "url": "https://picsum.photos/id/1070/800/600"
                  },
                  {
                    "file_id": "evidence-7-2",
                    "original_name": "instalacion-reparada.jpg",
                    "url": "https://picsum.photos/id/1080/800/600"
                  },
                  {
                    "file_id": "evidence-7-3",
                    "original_name": "tablero-final.jpg",
                    "url": "https://picsum.photos/id/1060/800/600"
                  }
                ]
              },
              "review": null
            }
        """.trimIndent()
    }
}

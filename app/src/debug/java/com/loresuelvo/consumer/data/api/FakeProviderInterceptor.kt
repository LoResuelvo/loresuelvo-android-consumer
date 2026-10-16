package com.loresuelvo.consumer.data.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class FakeProviderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (request.method != "GET") return chain.proceed(request)

        return when {
            path == "/providers" &&
                request.url.queryParameter("category_id") == ELECTRICITY_CATEGORY_ID.toString() ->
                response(request, MOCK_ELECTRICIAN_LIST_JSON)

            path == "/providers/$MOCK_PROVIDER_ID" ->
                response(request, MOCK_ELECTRICIAN_PROFILE_JSON)

            else -> chain.proceed(request)
        }
    }

    private fun response(
        request: okhttp3.Request,
        body: String,
    ): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody(JSON_MEDIA_TYPE))
        .addHeader("Content-Type", "application/json")
        .build()

    private companion object {
        const val ELECTRICITY_CATEGORY_ID = 2
        const val MOCK_PROVIDER_ID = 909
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val MOCK_ELECTRICIAN_LIST_JSON = """
            [
              {
                "id": 909,
                "name": "Mariano",
                "surname": "Rossi",
                "category_name": "Electricidad",
                "profile_photo_url": null,
                "rating_average": 2.3,
                "rating_count": 10,
                "identity_verified": false
              }
            ]
        """.trimIndent()

        val MOCK_ELECTRICIAN_PROFILE_JSON = """
            {
              "id": 909,
              "name": "Mariano",
              "surname": "Rossi",
              "profile_photo": null,
              "category": {
                "id": 2,
                "name": "Electricidad"
              },
              "rating_average": 2.3,
              "rating_count": 10,
              "identity_verified": false,
              "work_orders": [
                {
                  "id": 90901,
                  "scheduled_on": "2026-10-10T14:00:00Z",
                  "description": "Diagnóstico y reparación de tablero eléctrico",
                  "status": "finished",
                  "completion_report": {
                    "description": "Trabajo finalizado y probado correctamente.",
                    "reported_on": "2026-10-10T17:00:00Z"
                  },
                  "review": {
                    "rating": 2,
                    "description": "El trabajo quedó resuelto."
                  }
                }
              ]
            }
        """.trimIndent()
    }
}

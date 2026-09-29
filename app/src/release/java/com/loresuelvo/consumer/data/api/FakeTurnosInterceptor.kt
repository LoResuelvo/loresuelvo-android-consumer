package com.loresuelvo.consumer.data.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Release-source-set counterpart of the debug-only turnos fake.
 *
 * The production network module is shared with debug variants and therefore
 * needs this type to compile in release variants. MOCK_TURNOS is false for
 * every release flavor, so this implementation is never installed in a
 * release OkHttp client.
 */
class FakeTurnosInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}

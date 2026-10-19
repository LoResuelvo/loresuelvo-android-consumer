package com.loresuelvo.consumer.data.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Release-source-set counterpart of the debug-only provider fake.
 *
 * The shared network module references this type, but provider mocks are
 * disabled in release flavors. Passing requests through keeps real API data
 * in release builds while allowing the shared module to compile.
 */
class FakeProviderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}

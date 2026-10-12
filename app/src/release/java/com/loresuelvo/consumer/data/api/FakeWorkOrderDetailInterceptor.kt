package com.loresuelvo.consumer.data.api

import okhttp3.Interceptor
import okhttp3.Response

class FakeWorkOrderDetailInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}

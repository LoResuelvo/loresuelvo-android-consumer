package com.loresuelvo.consumer.data.api

import okhttp3.Interceptor
import okhttp3.Response

class FakeServiceProposalsInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}

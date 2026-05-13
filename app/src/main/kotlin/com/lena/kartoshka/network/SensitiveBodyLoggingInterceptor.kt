package com.lena.kartoshka.network

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

class SensitiveBodyLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isSensitivePath = request.url.encodedPath.contains("/auth/")
        val tag = "OkHttp-Body"
        if (isSensitivePath) {
            android.util.Log.d(tag, "${request.method} ${request.url.encodedPath} → [body redacted]")
        } else {
            val body = request.body
            val contentType = body?.contentType()
            // Only log application/json — skip multipart and other streaming bodies
            if (body != null && !body.isOneShot() &&
                contentType?.type == "application" && contentType.subtype == "json"
            ) {
                val buffer = Buffer()
                body.writeTo(buffer)
                android.util.Log.d(tag, "${request.method} ${request.url.encodedPath} → ${buffer.readUtf8()}")
            }
        }
        return chain.proceed(request)
    }
}

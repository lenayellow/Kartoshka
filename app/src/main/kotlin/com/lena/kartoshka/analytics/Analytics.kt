package com.lena.kartoshka.analytics

import io.appmetrica.analytics.AppMetrica
import com.lena.kartoshka.BuildConfig

object Analytics {

    private val enabled: Boolean get() = BuildConfig.APPMETRICA_API_KEY.isNotEmpty()

    fun trackEvent(name: String, params: Map<String, Any>? = null) {
        if (!enabled) return
        if (params != null) {
            AppMetrica.reportEvent(name, params)
        } else {
            AppMetrica.reportEvent(name)
        }
    }

    fun trackError(throwable: Throwable, message: String? = null) {
        if (!enabled) return
        AppMetrica.reportError(message ?: throwable.message ?: "unknown error", throwable)
    }

    fun setUserId(userId: String?) {
        if (!enabled) return
        AppMetrica.setUserProfileID(userId)
    }
}

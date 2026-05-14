package com.lena.kartoshka

import android.app.Application
import android.util.Log
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig

class SuperListsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initAppMetrica()
    }

    private fun initAppMetrica() {
        val apiKey = BuildConfig.APPMETRICA_API_KEY
        if (apiKey.isEmpty()) {
            Log.w("SuperListsApp", "APPMETRICA_API_KEY is not set — crash reporting disabled")
            return
        }

        // TODO(sprint-6, privacy): gate AppMetrica init behind user consent screen
        // before first run. Currently consent is implicit on install, which is
        // non-compliant with 152-ФЗ for production.
        val config = AppMetricaConfig.newConfigBuilder(apiKey)
            .withCrashReporting(true)
            .withNativeCrashReporting(true)
            .apply { if (BuildConfig.DEBUG) withLogs() }
            .build()

        AppMetrica.activate(this, config)
        AppMetrica.enableActivityAutoTracking(this)
    }
}

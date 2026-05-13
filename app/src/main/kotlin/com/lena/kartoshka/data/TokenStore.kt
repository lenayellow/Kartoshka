package com.lena.kartoshka.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class TokenStore(context: Context) {

    enum class LogoutReason { SESSION_EXPIRED, USER_INITIATED }

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs = EncryptedSharedPreferences.create(
        "kartoshka_tokens",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _logoutEvents = MutableSharedFlow<LogoutReason>(extraBufferCapacity = 1)
    val logoutEvents: SharedFlow<LogoutReason> = _logoutEvents.asSharedFlow()

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) = prefs.edit().putString("access_token", value).apply()

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) = prefs.edit().putString("refresh_token", value).apply()

    fun clear() = prefs.edit().clear().apply()

    fun clearWithReason(reason: LogoutReason) {
        prefs.edit().clear().apply()
        _logoutEvents.tryEmit(reason)
    }

    val isLoggedIn: Boolean get() = accessToken != null
}

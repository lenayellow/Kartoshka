package com.lena.kartoshka.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.IOException
import java.security.GeneralSecurityException

class TokenStore(context: Context) {

    companion object {
        private const val PREFS_NAME = "kartoshka_tokens"

        // InvalidProtocolBufferException (subtype of IOException) is thrown when the Tink keyset
        // in the prefs file was encrypted with a Keystore key that no longer exists — typically
        // after a system backup restore or Tink library upgrade. Deleting the prefs file also
        // removes the embedded keyset, so a fresh create() succeeds. The user is logged out,
        // which is acceptable: the stored tokens were unreadable anyway.
        private fun openPrefs(context: Context, masterKeyAlias: String): SharedPreferences? = try {
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: IOException) {
            Log.w("TokenStore", "EncryptedSharedPreferences open failed: ${e.javaClass.simpleName}")
            null
        } catch (e: GeneralSecurityException) {
            Log.w("TokenStore", "EncryptedSharedPreferences open failed: ${e.javaClass.simpleName}")
            null
        }
    }

    enum class LogoutReason { SESSION_EXPIRED, USER_INITIATED }

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs: SharedPreferences = openPrefs(context, masterKeyAlias) ?: run {
        context.deleteSharedPreferences(PREFS_NAME)
        checkNotNull(openPrefs(context, masterKeyAlias)) {
            "EncryptedSharedPreferences unrecoverable after prefs reset"
        }
    }

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

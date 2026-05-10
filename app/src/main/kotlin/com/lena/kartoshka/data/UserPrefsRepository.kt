package com.lena.kartoshka.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPrefsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _isDark = MutableStateFlow(prefs.getBoolean("is_dark", true))
    val isDark: StateFlow<Boolean> = _isDark.asStateFlow()

    private val _avatarPath = MutableStateFlow<String?>(prefs.getString("avatar_path", null))
    val avatarPath: StateFlow<String?> = _avatarPath.asStateFlow()

    fun setDark(dark: Boolean) {
        prefs.edit().putBoolean("is_dark", dark).apply()
        _isDark.value = dark
    }

    fun saveAvatarPath(path: String?) {
        prefs.edit().putString("avatar_path", path).apply()
        _avatarPath.value = path
    }
}

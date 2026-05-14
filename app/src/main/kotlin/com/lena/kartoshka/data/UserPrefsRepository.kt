package com.lena.kartoshka.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPrefsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.find { it.name == prefs.getString("theme_mode", null) } ?: ThemeMode.SYSTEM
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _avatarPath = MutableStateFlow<String?>(prefs.getString("avatar_path", null))
    val avatarPath: StateFlow<String?> = _avatarPath.asStateFlow()

    private val _userName = MutableStateFlow<String?>(prefs.getString("user_name", null))
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(prefs.getString("user_email", null))
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun saveAvatarPath(path: String?) {
        prefs.edit().putString("avatar_path", path).apply()
        _avatarPath.value = path
    }

    fun saveUserInfo(name: String, email: String) {
        prefs.edit().putString("user_name", name).putString("user_email", email).apply()
        _userName.value = name
        _userEmail.value = email
    }

    fun clearUserInfo() {
        prefs.edit().remove("user_name").remove("user_email").apply()
        _userName.value = null
        _userEmail.value = null
    }
}

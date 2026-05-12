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

    private val _userName = MutableStateFlow<String?>(prefs.getString("user_name", null))
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(prefs.getString("user_email", null))
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    fun setDark(dark: Boolean) {
        prefs.edit().putBoolean("is_dark", dark).apply()
        _isDark.value = dark
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

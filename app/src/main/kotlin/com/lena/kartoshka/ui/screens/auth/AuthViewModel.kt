package com.lena.kartoshka.ui.screens.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lena.kartoshka.analytics.Analytics
import com.lena.kartoshka.data.TokenStore
import com.lena.kartoshka.network.ApiService
import com.lena.kartoshka.network.EmailLoginRequest
import com.lena.kartoshka.network.EmailRegisterRequest
import com.lena.kartoshka.network.ForgotPasswordRequest
import com.lena.kartoshka.network.YandexLoginRequest
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AuthViewModel(
    private val api: ApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    enum class Mode { LOGIN, REGISTER }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class LoginSuccess(val name: String, val email: String) : UiState()
        data class Error(val message: String) : UiState()
        object ForgotPasswordSent : UiState()
    }

    var mode by mutableStateOf(Mode.LOGIN)
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var name by mutableStateOf("")

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("AuthViewModel", "Unhandled coroutine exception", throwable)
        Analytics.trackError(throwable, "AuthViewModel: unhandled coroutine failure")
    }

    fun switchMode(newMode: Mode) {
        mode = newMode
        _state.value = UiState.Idle
    }

    fun submit(
        errorNoConnection: String,
        errorWrongCredentials: String,
        errorEmailTaken: String,
        errorNotVerified: String,
        errorShortPassword: String,
        errorGeneric: String
    ) {
        if (mode == Mode.REGISTER && password.length < 8) {
            _state.value = UiState.Error(errorShortPassword)
            return
        }

        viewModelScope.launch(exceptionHandler) {
            _state.value = UiState.Loading
            try {
                val tokens = if (mode == Mode.LOGIN) {
                    api.login(EmailLoginRequest(email.trim(), password))
                } else {
                    api.register(EmailRegisterRequest(email.trim(), password, name.trim()))
                }
                tokenStore.accessToken = tokens.access_token
                tokenStore.refreshToken = tokens.refresh_token
                val profile = runCatching { api.getMe() }.getOrNull()
                Analytics.setUserId(email.trim().lowercase())
                _state.value = UiState.LoginSuccess(
                    name = profile?.name ?: if (mode == Mode.REGISTER) name.trim() else "",
                    email = profile?.email ?: email.trim()
                )
            } catch (e: HttpException) {
                Analytics.trackError(e, "AuthViewModel: HTTP ${e.code()} on email auth")
                val msg = when (e.code()) {
                    401 -> errorWrongCredentials
                    403 -> errorNotVerified
                    409 -> errorEmailTaken
                    else -> errorGeneric
                }
                _state.value = UiState.Error(msg)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email auth network error", e)
                Analytics.trackError(e, "AuthViewModel: network error on email auth")
                _state.value = UiState.Error(errorNoConnection)
            }
        }
    }

    fun loginWithYandex(code: String, errorNoConnection: String) {
        viewModelScope.launch(exceptionHandler) {
            _state.value = UiState.Loading
            try {
                val tokens = api.loginYandex(YandexLoginRequest(code))
                tokenStore.accessToken = tokens.access_token
                tokenStore.refreshToken = tokens.refresh_token
                val profile = runCatching { api.getMe() }.getOrNull()
                Analytics.setUserId(profile?.user_id)
                _state.value = UiState.LoginSuccess(
                    name = profile?.name ?: "",
                    email = profile?.email ?: ""
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Yandex login failed", e)
                Analytics.trackError(e, "AuthViewModel: Yandex login failed")
                _state.value = UiState.Error(errorNoConnection)
            }
        }
    }

    fun forgotPassword(email: String, errorNotFound: String, errorNoConnection: String) {
        viewModelScope.launch(exceptionHandler) {
            _state.value = UiState.Loading
            try {
                api.forgotPassword(ForgotPasswordRequest(email.trim()))
                _state.value = UiState.ForgotPasswordSent
            } catch (e: HttpException) {
                Analytics.trackError(e, "AuthViewModel: HTTP ${e.code()} on forgot password")
                val msg = if (e.code() == 404) errorNotFound else errorNoConnection
                _state.value = UiState.Error(msg)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Forgot password request failed", e)
                Analytics.trackError(e, "AuthViewModel: network error on forgot password")
                _state.value = UiState.Error(errorNoConnection)
            }
        }
    }

    fun clearError() {
        if (_state.value is UiState.Error || _state.value is UiState.ForgotPasswordSent) {
            _state.value = UiState.Idle
        }
    }

    class Factory(
        private val api: ApiService,
        private val tokenStore: TokenStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(api, tokenStore) as T
    }
}

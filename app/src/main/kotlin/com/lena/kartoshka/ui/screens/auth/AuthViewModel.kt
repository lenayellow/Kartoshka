package com.lena.kartoshka.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lena.kartoshka.data.TokenStore
import com.lena.kartoshka.network.ApiService
import com.lena.kartoshka.network.EmailLoginRequest
import com.lena.kartoshka.network.EmailRegisterRequest
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
        object LoginSuccess : UiState()
        data class RegisterSuccess(val email: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    var mode by mutableStateOf(Mode.LOGIN)
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var name by mutableStateOf("")

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

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

        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val tokens = if (mode == Mode.LOGIN) {
                    api.login(EmailLoginRequest(email.trim(), password))
                } else {
                    api.register(EmailRegisterRequest(email.trim(), password, name.trim()))
                }
                tokenStore.accessToken = tokens.access_token
                tokenStore.refreshToken = tokens.refresh_token
                _state.value = UiState.LoginSuccess
            } catch (e: HttpException) {
                val msg = when (e.code()) {
                    401 -> errorWrongCredentials
                    403 -> errorNotVerified
                    409 -> errorEmailTaken
                    else -> errorGeneric
                }
                _state.value = UiState.Error(msg)
            } catch (e: Exception) {
                _state.value = UiState.Error(errorNoConnection)
            }
        }
    }

    fun clearError() {
        if (_state.value is UiState.Error) _state.value = UiState.Idle
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

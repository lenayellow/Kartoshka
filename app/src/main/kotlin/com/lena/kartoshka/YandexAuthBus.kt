package com.lena.kartoshka

import kotlinx.coroutines.flow.MutableStateFlow

object YandexAuthBus {
    val code = MutableStateFlow<String?>(null)
}

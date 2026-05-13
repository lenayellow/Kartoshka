package com.lena.kartoshka.network

import com.google.gson.Gson
import com.lena.kartoshka.BuildConfig
import com.lena.kartoshka.data.TokenStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private lateinit var tokenStore: TokenStore

    fun init(store: TokenStore) {
        tokenStore = store
    }

    // Отдельный клиент без авторизации — только для рефреша токена
    private val plainClient = OkHttpClient.Builder().build()

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = if (::tokenStore.isInitialized) tokenStore.accessToken else null
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .authenticator { _, response ->
                // Не повторяем если уже пробовали рефреш
                if (response.request.header("X-Retry") != null) return@authenticator null
                if (!::tokenStore.isInitialized) return@authenticator null
                val rt = tokenStore.refreshToken ?: return@authenticator null

                val newTokens = try {
                    val body = """{"refresh_token":"$rt"}"""
                        .toRequestBody("application/json".toMediaType())
                    val req = Request.Builder()
                        .url("${BuildConfig.API_BASE_URL}auth/refresh")
                        .post(body)
                        .build()
                    val resp = plainClient.newCall(req).execute()
                    if (!resp.isSuccessful) {
                        tokenStore.clearWithReason(TokenStore.LogoutReason.SESSION_EXPIRED)
                        return@authenticator null
                    }
                    val json = resp.body?.string() ?: return@authenticator null
                    Gson().fromJson(json, TokenPair::class.java)
                } catch (e: Exception) {
                    tokenStore.clearWithReason(TokenStore.LogoutReason.SESSION_EXPIRED)
                    return@authenticator null
                }

                tokenStore.accessToken = newTokens.access_token
                tokenStore.refreshToken = newTokens.refresh_token

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.access_token}")
                    .header("X-Retry", "true")
                    .build()
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(SensitiveBodyLoggingInterceptor())
                    val logging = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                        redactHeader("Authorization")
                        redactHeader("Cookie")
                    }
                    addInterceptor(logging)
                }
            }
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

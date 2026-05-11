package com.lena.kartoshka.network

import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/email/register")
    suspend fun register(@Body body: EmailRegisterRequest): TokenPair

    @POST("auth/email/login")
    suspend fun login(@Body body: EmailLoginRequest): TokenPair

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenPair

    @POST("auth/logout")
    suspend fun logout()

    // User
    @GET("users/me")
    suspend fun getMe(): UserResponse

    // Lists
    @GET("lists")
    suspend fun getLists(): List<ApiList>

    @GET("lists/{list_id}/items")
    suspend fun getItems(@Path("list_id") listId: String): List<ApiItem>
}

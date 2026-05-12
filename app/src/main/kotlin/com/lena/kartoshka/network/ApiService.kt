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
    suspend fun logout(@Body body: LogoutRequest)

    // User
    @GET("users/me")
    suspend fun getMe(): UserResponse

    // Lists
    @GET("lists")
    suspend fun getLists(): List<ApiList>

    @POST("lists")
    suspend fun createList(@Body body: CreateListRequest): ApiList

    @PUT("lists/{list_id}")
    suspend fun updateList(@Path("list_id") listId: String, @Body body: UpdateListRequest): ApiList

    @DELETE("lists/{list_id}")
    suspend fun deleteList(@Path("list_id") listId: String)

    // Items
    @GET("lists/{list_id}/members")
    suspend fun getMembers(@Path("list_id") listId: String): List<ListMemberResponse>

    @POST("lists/{list_id}/invite")
    suspend fun createInvite(@Path("list_id") listId: String, @Body body: CreateInviteRequest): CreateInviteResponse

    @GET("lists/{list_id}/items")
    suspend fun getItems(@Path("list_id") listId: String): List<ApiItem>

    @POST("lists/{list_id}/items")
    suspend fun createItem(@Path("list_id") listId: String, @Body body: CreateItemRequest): ApiItem

    @PUT("lists/{list_id}/items/{item_id}")
    suspend fun updateItem(
        @Path("list_id") listId: String,
        @Path("item_id") itemId: String,
        @Body body: UpdateItemRequest
    ): ApiItem

    @DELETE("lists/{list_id}/items/{item_id}")
    suspend fun deleteItem(@Path("list_id") listId: String, @Path("item_id") itemId: String)
}

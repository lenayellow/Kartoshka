package com.lena.kartoshka.network

data class TokenPair(
    val access_token: String,
    val refresh_token: String
)

data class EmailRegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

data class EmailLoginRequest(
    val email: String,
    val password: String
)

data class RefreshRequest(
    val refresh_token: String
)

data class LogoutRequest(
    val refresh_token: String
)

data class RegisterResponse(
    val message: String,
    val user_id: String
)

data class UserResponse(
    val user_id: String,
    val email: String,
    val name: String,
    val avatar_url: String?
)

data class CreateListRequest(
    val title: String,
    val color_value: Long,
    val position: Int
)

data class UpdateListRequest(
    val title: String,
    val color_value: Long,
    val position: Int = 0,
    val category_order: String = "",
    val hidden_categories: String = ""
)

data class CreateItemRequest(
    val name: String,
    val tags: String = "",
    val note: String = "",
    val category_id: String = "",
    val sort_index: Int = 0
)

data class UpdateItemRequest(
    val name: String,
    val tags: String = "",
    val note: String = "",
    val category_id: String = "",
    val sort_index: Int = 0
)

data class ApiList(
    val list_id: String,
    val title: String,
    val color_value: Long,
    val position: Int,
    val created_at: String,
    val updated_at: String
)

data class ApiItem(
    val item_id: String,
    val list_id: String,
    val name: String,
    val note: String?,
    val tags: String?,
    val category_id: String?,
    val is_deleted: Boolean,
    val sort_index: Int,
    val updated_at: String
)

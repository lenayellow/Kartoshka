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

data class ApiList(
    val list_id: String,
    val title: String,
    val color_value: Int?,
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
    val sort_index: Long,
    val updated_at: String
)

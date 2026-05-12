package com.lena.kartoshka.data

data class ListMember(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val role: String
)

data class InviteResult(val webLink: String, val deepLink: String, val emailSent: Boolean)

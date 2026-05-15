package com.lena.kartoshka.network

import android.content.Context
import com.google.gson.Gson
import com.lena.kartoshka.R
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class Feature { Generic, Share, Auth, Lists, Items }

sealed class NetworkError {
    abstract val httpCode: Int?
    abstract val serverMessage: String?
    abstract val errorCode: String?

    data class NoConnection(
        override val httpCode: Int? = null,
        override val serverMessage: String? = null,
        override val errorCode: String? = null
    ) : NetworkError()

    data class Unauthorized(
        override val httpCode: Int? = 401,
        override val serverMessage: String? = null,
        override val errorCode: String? = null
    ) : NetworkError()

    data class Forbidden(
        override val httpCode: Int? = 403,
        override val serverMessage: String? = null,
        override val errorCode: String? = null
    ) : NetworkError()

    data class NotFound(
        override val httpCode: Int? = 404,
        override val serverMessage: String? = null,
        override val errorCode: String? = null
    ) : NetworkError()

    data class Conflict(
        override val httpCode: Int? = 409,
        override val serverMessage: String? = null,
        override val errorCode: String? = null
    ) : NetworkError()

    data class Gone(
        override val httpCode: Int? = 410,
        override val serverMessage: String? = null,
        override val errorCode: String? = null
    ) : NetworkError()

    data class TooManyRequests(
        override val httpCode: Int? = 429,
        override val serverMessage: String? = null,
        override val errorCode: String? = null
    ) : NetworkError()

    data class ClientError(
        override val httpCode: Int? = null,
        override val serverMessage: String? = null,
        override val errorCode: String? = null
    ) : NetworkError()

    data class ServerError(
        override val httpCode: Int? = null,
        override val serverMessage: String? = null,
        override val errorCode: String? = null
    ) : NetworkError()

    data class Unknown(
        override val httpCode: Int? = null,
        override val serverMessage: String? = null,
        override val errorCode: String? = null
    ) : NetworkError()
}

fun Throwable.toNetworkError(): NetworkError = when (this) {
    is HttpException -> {
        val code = code()
        val body = try { response()?.errorBody()?.string() } catch (ex: Exception) { null }
        val errorCode = parseErrorCode(body)
        when (code) {
            401 -> NetworkError.Unauthorized(code, body, errorCode)
            403 -> NetworkError.Forbidden(code, body, errorCode)
            404 -> NetworkError.NotFound(code, body, errorCode)
            409 -> NetworkError.Conflict(code, body, errorCode)
            410 -> NetworkError.Gone(code, body, errorCode)
            429 -> NetworkError.TooManyRequests(code, body, errorCode)
            in 400..499 -> NetworkError.ClientError(code, body, errorCode)
            in 500..599 -> NetworkError.ServerError(code, body, errorCode)
            else -> NetworkError.Unknown(code, body, errorCode)
        }
    }
    is SocketTimeoutException -> NetworkError.NoConnection()
    is ConnectException -> NetworkError.NoConnection()
    is UnknownHostException -> NetworkError.NoConnection()
    is IOException -> NetworkError.NoConnection()
    else -> NetworkError.Unknown(serverMessage = message)
}

fun NetworkError.isRetryable() = when (this) {
    is NetworkError.NoConnection,
    is NetworkError.ServerError,
    is NetworkError.TooManyRequests,
    is NetworkError.Unknown -> true
    else -> false
}

private fun parseErrorCode(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return try {
        Gson().fromJson(body, ApiErrorBody::class.java)?.error?.code
    } catch (e: Exception) {
        null
    }
}

fun NetworkError.toUserMessage(context: Context, feature: Feature = Feature.Generic): String =
    when (feature) {
        Feature.Share -> when (this) {
            is NetworkError.NoConnection -> context.getString(R.string.share_error_no_connection)
            is NetworkError.NotFound -> when (errorCode) {
                "invite_user_not_found" -> context.getString(R.string.share_error_user_not_found)
                "list_not_found" -> context.getString(R.string.error_not_found)
                else -> context.getString(R.string.share_error_user_not_found)
            }
            is NetworkError.Conflict -> when (errorCode) {
                "invite_self_forbidden" -> context.getString(R.string.share_error_self_invite)
                "invite_already_sent" -> context.getString(R.string.share_error_already_invited)
                "invite_already_member" -> context.getString(R.string.share_error_already_member)
                else -> context.getString(R.string.share_error_already_invited)
            }
            is NetworkError.Gone -> when (errorCode) {
                "invite_expired" -> context.getString(R.string.share_error_invite_expired)
                else -> context.getString(R.string.error_unknown)
            }
            is NetworkError.Unauthorized -> context.getString(R.string.error_session_expired)
            is NetworkError.TooManyRequests -> context.getString(R.string.error_too_many_requests)
            is NetworkError.ServerError -> context.getString(R.string.share_error_server)
            else -> context.getString(R.string.error_unknown)
        }
        Feature.Auth -> when (this) {
            is NetworkError.NoConnection -> context.getString(R.string.auth_error_no_connection)
            is NetworkError.Unauthorized -> when (errorCode) {
                "invalid_credentials" -> context.getString(R.string.auth_error_wrong_credentials)
                else -> context.getString(R.string.auth_error_wrong_credentials)
            }
            is NetworkError.Conflict -> when (errorCode) {
                "email_taken" -> context.getString(R.string.auth_error_email_taken)
                else -> context.getString(R.string.auth_error_generic)
            }
            is NetworkError.NotFound -> when (errorCode) {
                "email_not_found" -> context.getString(R.string.auth_error_email_not_found)
                else -> context.getString(R.string.auth_error_generic)
            }
            is NetworkError.ClientError -> when (errorCode) {
                "weak_password" -> context.getString(R.string.auth_error_short_password)
                "email_not_verified" -> context.getString(R.string.auth_error_not_verified)
                else -> context.getString(R.string.auth_error_generic)
            }
            is NetworkError.TooManyRequests -> context.getString(R.string.error_too_many_requests)
            is NetworkError.ServerError -> context.getString(R.string.error_server_unavailable)
            else -> context.getString(R.string.auth_error_generic)
        }
        Feature.Items -> when (this) {
            is NetworkError.NoConnection -> context.getString(R.string.error_no_connection)
            is NetworkError.Unauthorized -> context.getString(R.string.error_session_expired)
            is NetworkError.ServerError -> context.getString(R.string.error_server_unavailable)
            else -> context.getString(R.string.error_unknown)
        }
        Feature.Lists -> when (this) {
            is NetworkError.NoConnection -> context.getString(R.string.error_no_connection)
            is NetworkError.Unauthorized -> context.getString(R.string.error_session_expired)
            is NetworkError.ServerError -> context.getString(R.string.error_server_unavailable)
            else -> context.getString(R.string.error_unknown)
        }
        Feature.Generic -> when (this) {
            is NetworkError.NoConnection -> context.getString(R.string.error_no_connection)
            is NetworkError.Unauthorized -> context.getString(R.string.error_session_expired)
            is NetworkError.TooManyRequests -> context.getString(R.string.error_too_many_requests)
            is NetworkError.ServerError -> context.getString(R.string.error_server_unavailable)
            else -> context.getString(R.string.error_unknown)
        }
    }

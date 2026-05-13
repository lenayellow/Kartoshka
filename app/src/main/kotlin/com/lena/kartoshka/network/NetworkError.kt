package com.lena.kartoshka.network

import android.content.Context
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

    data class NoConnection(
        override val httpCode: Int? = null,
        override val serverMessage: String? = null
    ) : NetworkError()

    data class Unauthorized(
        override val httpCode: Int? = 401,
        override val serverMessage: String? = null
    ) : NetworkError()

    data class Forbidden(
        override val httpCode: Int? = 403,
        override val serverMessage: String? = null
    ) : NetworkError()

    data class NotFound(
        override val httpCode: Int? = 404,
        override val serverMessage: String? = null
    ) : NetworkError()

    data class Conflict(
        override val httpCode: Int? = 409,
        override val serverMessage: String? = null
    ) : NetworkError()

    data class TooManyRequests(
        override val httpCode: Int? = 429,
        override val serverMessage: String? = null
    ) : NetworkError()

    data class ClientError(
        override val httpCode: Int? = null,
        override val serverMessage: String? = null
    ) : NetworkError()

    data class ServerError(
        override val httpCode: Int? = null,
        override val serverMessage: String? = null
    ) : NetworkError()

    data class Unknown(
        override val httpCode: Int? = null,
        override val serverMessage: String? = null
    ) : NetworkError()
}

fun Throwable.toNetworkError(): NetworkError = when (this) {
    is HttpException -> {
        val code = code()
        val body = try { response()?.errorBody()?.string() } catch (ex: Exception) { null }
        when (code) {
            401 -> NetworkError.Unauthorized(code, body)
            403 -> NetworkError.Forbidden(code, body)
            404 -> NetworkError.NotFound(code, body)
            409 -> NetworkError.Conflict(code, body)
            429 -> NetworkError.TooManyRequests(code, body)
            in 400..499 -> NetworkError.ClientError(code, body)
            in 500..599 -> NetworkError.ServerError(code, body)
            else -> NetworkError.Unknown(code, body)
        }
    }
    is SocketTimeoutException -> NetworkError.NoConnection()
    is ConnectException -> NetworkError.NoConnection()
    is UnknownHostException -> NetworkError.NoConnection()
    is IOException -> NetworkError.NoConnection()
    else -> NetworkError.Unknown(serverMessage = message)
}

fun NetworkError.toUserMessage(context: Context, feature: Feature = Feature.Generic): String =
    when (feature) {
        Feature.Share -> when (this) {
            is NetworkError.NoConnection -> context.getString(R.string.share_error_no_connection)
            is NetworkError.NotFound -> context.getString(R.string.share_error_user_not_found)
            is NetworkError.Conflict -> {
                val msg = serverMessage?.lowercase() ?: ""
                when {
                    msg.contains("already_member") || msg.contains("member") ->
                        context.getString(R.string.share_error_already_member)
                    msg.contains("self") ->
                        context.getString(R.string.share_error_self_invite)
                    else -> context.getString(R.string.share_error_already_invited)
                }
            }
            is NetworkError.Unauthorized -> context.getString(R.string.error_session_expired)
            is NetworkError.TooManyRequests -> context.getString(R.string.error_too_many_requests)
            is NetworkError.ServerError -> context.getString(R.string.share_error_server)
            else -> context.getString(R.string.error_unknown)
        }
        Feature.Auth -> when (this) {
            is NetworkError.NoConnection -> context.getString(R.string.auth_error_no_connection)
            is NetworkError.Unauthorized -> context.getString(R.string.auth_error_wrong_credentials)
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

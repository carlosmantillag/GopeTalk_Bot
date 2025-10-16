package com.example.gopetalk_bot.domain.entities

sealed class ApiResponse {
    data class Success(val statusCode: Int, val body: String) : ApiResponse()
    data class Error(val message: String, val exception: Throwable? = null) : ApiResponse()
}

package com.example.gopetalk_bot.domain.entities

import java.io.File

sealed class ApiResponse {
    data class Success(val statusCode: Int, val body: String, val audioFile: File? = null) : ApiResponse()
    data class Error(val message: String, val statusCode: Int? = null, val exception: Throwable? = null) : ApiResponse()
}

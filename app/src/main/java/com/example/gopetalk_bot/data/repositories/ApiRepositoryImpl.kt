package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.data.datasources.remote.RemoteDataSource
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.repositories.ApiRepository
import java.io.IOException

class ApiRepositoryImpl(
    private val remoteDataSource: RemoteDataSource,
    private val userPreferences: UserPreferences
) : ApiRepository {

    override fun sendAudioCommand(
        audioData: AudioData,
        userId: String,
        callback: (ApiResponse) -> Unit
    ) {
        remoteDataSource.sendAudioCommand(
            audioFile = audioData.file,
            userId = userId,
            authToken = userPreferences.authToken,
            callback = object : RemoteDataSource.ApiCallback {
                override fun onSuccess(statusCode: Int, body: String, audioFile: java.io.File?) {
                    callback(ApiResponse.Success(statusCode, body, audioFile))
                }

                override fun onFailure(e: IOException) {
                    callback(ApiResponse.Error(e.message ?: "Unknown error", e))
                }
            }
        )
    }
    
    override fun sendAuthentication(
        nombre: String,
        pin: Int,
        callback: (ApiResponse) -> Unit
    ) {
        remoteDataSource.sendAuthentication(
            nombre = nombre,
            pin = pin,
            callback = object : RemoteDataSource.AuthCallback {
                override fun onSuccess(statusCode: Int, message: String, token: String) {
                    // Return both message and token in the body as JSON-like string
                    val responseBody = "{\"message\":\"$message\",\"token\":\"$token\"}"
                    callback(ApiResponse.Success(statusCode, responseBody, null))
                }
                
                override fun onFailure(e: IOException) {
                    callback(ApiResponse.Error(e.message ?: "Unknown error", e))
                }
            }
        )
    }
}

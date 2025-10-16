package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.remote.RemoteDataSource
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.repositories.ApiRepository
import java.io.IOException

/**
 * Implementation of ApiRepository
 */
class ApiRepositoryImpl(
    private val remoteDataSource: RemoteDataSource
) : ApiRepository {

    override fun sendAudioCommand(
        audioData: AudioData,
        userId: String,
        callback: (ApiResponse) -> Unit
    ) {
        remoteDataSource.sendAudioCommand(
            audioFile = audioData.file,
            userId = userId,
            callback = object : RemoteDataSource.ApiCallback {
                override fun onSuccess(statusCode: Int, body: String) {
                    callback(ApiResponse.Success(statusCode, body))
                }

                override fun onFailure(e: IOException) {
                    callback(ApiResponse.Error(e.message ?: "Unknown error", e))
                }
            }
        )
    }
}

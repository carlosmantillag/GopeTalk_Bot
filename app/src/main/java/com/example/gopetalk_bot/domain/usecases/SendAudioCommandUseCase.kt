package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.repositories.ApiRepository

/**
 * Use case for sending audio commands to backend
 */
class SendAudioCommandUseCase(
    private val apiRepository: ApiRepository
) {
    fun execute(
        audioData: AudioData,
        userId: String,
        callback: (ApiResponse) -> Unit
    ) {
        apiRepository.sendAudioCommand(audioData, userId, callback)
    }
}

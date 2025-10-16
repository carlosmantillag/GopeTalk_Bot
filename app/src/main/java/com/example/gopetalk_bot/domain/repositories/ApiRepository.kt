package com.example.gopetalk_bot.domain.repositories

import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData

/**
 * Repository interface for API operations
 */
interface ApiRepository {
    /**
     * Send audio command to backend
     * @param audioData The audio data to send
     * @param userId The user identifier
     * @param callback Callback for the result
     */
    fun sendAudioCommand(
        audioData: AudioData,
        userId: String,
        callback: (ApiResponse) -> Unit
    )
}

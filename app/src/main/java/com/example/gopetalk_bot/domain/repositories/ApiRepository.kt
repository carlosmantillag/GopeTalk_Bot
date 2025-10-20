package com.example.gopetalk_bot.domain.repositories

import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData

interface ApiRepository {
    fun sendAudioCommand(
        audioData: AudioData,
        userId: String,
        callback: (ApiResponse) -> Unit
    )
    
    fun sendAuthentication(
        nombre: String,
        pin: Int,
        callback: (ApiResponse) -> Unit
    )
}

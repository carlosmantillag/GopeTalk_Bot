package com.example.gopetalk_bot.domain.repositories

import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData
import java.io.File

interface ApiRepository {
    fun sendAudioCommand(
        audioData: AudioData,
        callback: (ApiResponse) -> Unit
    )

    fun pollAudio(
        onAudioReceived: (File, String, String) -> Unit,
        onNoAudio: () -> Unit,
        onError: (String) -> Unit
    )

    fun sendAuthentication(
        nombre: String,
        pin: Int,
        callback: (ApiResponse) -> Unit
    )
}

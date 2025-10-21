package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.ApiRepository
import java.io.File

class PollAudioUseCase(
    private val apiRepository: ApiRepository
) {
    fun execute(
        onAudioReceived: (File, String, String) -> Unit,
        onNoAudio: () -> Unit,
        onError: (String) -> Unit
    ) {
        apiRepository.pollAudio(onAudioReceived, onNoAudio, onError)
    }
}

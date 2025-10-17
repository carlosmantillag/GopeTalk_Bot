package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.TextToSpeechRepository

class ShutdownTtsUseCase(
    private val textToSpeechRepository: TextToSpeechRepository
) {
    fun execute() {
        textToSpeechRepository.shutdown()
    }
}

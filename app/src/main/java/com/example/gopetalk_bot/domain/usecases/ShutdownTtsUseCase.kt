package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.TextToSpeechRepository

/**
 * Use case for shutting down the TTS engine
 */
class ShutdownTtsUseCase(
    private val textToSpeechRepository: TextToSpeechRepository
) {
    fun execute() {
        textToSpeechRepository.shutdown()
    }
}

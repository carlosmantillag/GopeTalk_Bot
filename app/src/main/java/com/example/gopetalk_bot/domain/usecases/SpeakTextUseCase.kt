package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.TextToSpeechRepository

/**
 * Use case for speaking text using TTS
 */
class SpeakTextUseCase(
    private val textToSpeechRepository: TextToSpeechRepository
) {
    /**
     * Speak the given text
     * @param text Text to speak
     * @param utteranceId Unique identifier for this speech
     */
    fun execute(text: String, utteranceId: String = "server_response") {
        if (text.isNotBlank() && textToSpeechRepository.isInitialized()) {
            textToSpeechRepository.speak(text, utteranceId)
        }
    }
}

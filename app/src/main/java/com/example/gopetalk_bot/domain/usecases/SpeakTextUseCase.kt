package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.TextToSpeechRepository

class SpeakTextUseCase(
    private val textToSpeechRepository: TextToSpeechRepository
) {
    fun execute(text: String, utteranceId: String) {
        textToSpeechRepository.speak(text, utteranceId)
    }
}

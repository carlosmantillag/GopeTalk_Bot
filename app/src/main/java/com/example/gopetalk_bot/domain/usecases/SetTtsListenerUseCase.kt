package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.TextToSpeechRepository

class SetTtsListenerUseCase(
    private val textToSpeechRepository: TextToSpeechRepository
) {
    fun execute(
        onStart: (String?) -> Unit,
        onDone: (String?) -> Unit,
        onError: (String?) -> Unit
    ) {
        textToSpeechRepository.setUtteranceProgressListener(onStart, onDone, onError)
    }
}
